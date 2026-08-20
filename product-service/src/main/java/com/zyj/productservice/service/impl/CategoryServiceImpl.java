package com.zyj.productservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.AuthContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.BusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.result.PageResult;
import com.zyj.productservice.mapper.CategoryMapper;
import com.zyj.productservice.mapper.DishMapper;
import com.zyj.productservice.mapper.SetmealMapper;
import com.zyj.productservice.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 分类业务层
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final DishMapper dishMapper;
    private final SetmealMapper setmealMapper;
    private final RedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    // 缓存key前缀常量
    private static final String CATEGORY_CACHE_PREFIX = "categoryCache:";
    private static final long CATEGORY_CACHE_TTL = 60; // 分类变动较少，缓存时间更长

    /**
     * 新增分类（含权限校验+联合唯一校验+缓存清除）
     */
    public void save(CategoryDTO categoryDTO) {
        log.info("新增分类：{}", categoryDTO);
        // 【权限校验】非超管强制设置merchantId
        Long merchantId = resolveMerchantId(categoryDTO.getMerchantId());
        categoryDTO.setMerchantId(merchantId);

        // 【联合唯一校验】同一商家下分类名称不能重复
        checkNameUnique(merchantId, categoryDTO.getType(), categoryDTO.getName(), null);

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        category.setStatus(StatusConstant.DISABLE);
        categoryMapper.insert(category);

        // 【缓存清除】
        clearCategoryCache(merchantId, categoryDTO.getType());
    }

    /**
     * 分页查询（含权限过滤）
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        // searchCount=false：关闭自动 count（分页插件 count 绕过租户过滤），手动 count 修正 total
        Page<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize(), false);
        QueryWrapper<Category> queryWrapper = new QueryWrapper<>();

        if (categoryPageQueryDTO.getName() != null) {
            queryWrapper.like("name", categoryPageQueryDTO.getName());
        }
        if (categoryPageQueryDTO.getType() != null) {
            queryWrapper.eq("type", categoryPageQueryDTO.getType());
        }
        // 必须加 ORDER BY，否则 MySQL LIMIT 在无排序时返回结果集不确定，page>=2 可能为空
        queryWrapper.orderByDesc("update_time").orderByAsc("id");

        IPage<Category> categoryPage = categoryMapper.selectPage(page, queryWrapper);
        Long total = categoryMapper.selectCount(queryWrapper); // 走租户过滤链，修正 total
        categoryPage.setTotal(total == null ? 0 : total);
        return new PageResult(categoryPage.getTotal(), categoryPage.getRecords());
    }

    /**
     * 根据id删除分类（含权限校验+缓存清除）
     */
    public void deleteById(Long id) {
        log.info("根据id删除分类：{}", id);
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(category.getMerchantId());

        Integer count = dishMapper.countByCategoryId(id);
        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        count = setmealMapper.countByCategoryId(id);
        if (count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        removeById(id);

        // 【缓存清除】
        clearCategoryCache(category.getMerchantId(), category.getType());
    }

    /**
     * 修改分类（含权限校验+联合唯一校验+缓存清除）
     */
    public void update(CategoryDTO categoryDTO) {
        Long id = categoryDTO.getId();
        Category oldCategory = categoryMapper.selectById(id);
        if (oldCategory == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldCategory.getMerchantId());

        Long newMerchantId = resolveMerchantId(categoryDTO.getMerchantId());
        categoryDTO.setMerchantId(newMerchantId);

        // 【联合唯一校验】名称变更时检查
        if (!oldCategory.getName().equals(categoryDTO.getName())) {
            checkNameUnique(newMerchantId, categoryDTO.getType(), categoryDTO.getName(), id);
        }

        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO, category);
        updateById(category);

        // 【缓存清除】清除旧类型和新类型的缓存
        clearCategoryCache(newMerchantId, oldCategory.getType());
        if (!oldCategory.getType().equals(categoryDTO.getType())) {
            clearCategoryCache(newMerchantId, categoryDTO.getType());
        }
    }

    /**
     * 启用、禁用分类（含权限校验+缓存清除）
     */
    public void startOrStop(Integer status, Long id) {
        Category oldCategory = categoryMapper.selectById(id);
        if (oldCategory == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldCategory.getMerchantId());

        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        updateById(category);

        // 【缓存清除】
        clearCategoryCache(oldCategory.getMerchantId(), oldCategory.getType());
    }

    /**
     * 根据类型查询分类（admin端，含权限过滤）
     */
    public List<Category> list(Integer type) {
        log.info("根据类型查询分类：{}", type);
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(type != null, Category::getType, type);
        return list(queryWrapper);
    }

    /**
     * 用户端-查询分类列表（带Redis缓存）
     * key = categoryCache::{merchantId}::{type}
     */
    public List<Category> listWithCache(Long merchantId, Integer type) {
        if (type == null){
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(merchantId != null, Category::getMerchantId, merchantId)
                    .eq(Category::getStatus, StatusConstant.ENABLE);
            return list(queryWrapper);
        }

        String cacheKey = CATEGORY_CACHE_PREFIX + merchantId + "::" + type;
        log.info("用户端查询分类列表，key={}", cacheKey);

        // 【Redis缓存读取】
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("命中分类缓存，key={}", cacheKey);
            return (List<Category>) cached;
        }

        // 【Redisson互斥锁】防止缓存击穿，同一商家+类型同时只有一个线程查库写缓存
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            // Double-check：获取锁后再次检查缓存
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("双重检查命中分类缓存，key={}", cacheKey);
                return (List<Category>) cached;
            }

            // 缓存未命中，查数据库
            LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(type != null, Category::getType, type);
            queryWrapper.eq(Category::getMerchantId, merchantId);
            queryWrapper.eq(Category::getStatus, StatusConstant.ENABLE);
            List<Category> categories = list(queryWrapper);

            // 【Redis缓存写入】只缓存非空结果，避免空列表被缓存后一直命中空缓存
            if (categories != null && !categories.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, categories, CATEGORY_CACHE_TTL, TimeUnit.MINUTES);
                log.info("分类缓存已写入，key={}, size={}", cacheKey, categories.size());
            } else {
                log.info("分类查询结果为空，不写缓存，key={}", cacheKey);
            }

            return categories;
        } finally {
            lock.unlock();
        }
    }

    // ==================== 私有方法 ====================

    private void checkMerchantPermission(Long targetMerchantId) {
        if (!AuthContext.isSuperAdmin()) {
            Long currentMerchantId = AuthContext.getCurrentMerchantId();
            if (currentMerchantId == null || !currentMerchantId.equals(targetMerchantId)) {
                throw new BusinessException(MessageConstant.MERCHANT_PERMISSION_DENIED);
            }
        }
    }

    private Long resolveMerchantId(Long inputMerchantId) {
        if (!AuthContext.isSuperAdmin()) {
            return AuthContext.getCurrentMerchantId();
        }
        return inputMerchantId;
    }

    /**
     * 联合唯一校验：同一商家+同一类型下分类名称不能重复
     * @param merchantId 商家ID
     * @param type 类型
     * @param name 分类名称
     * @param excludeId 排除自身ID（修改时用）
     */
    private void checkNameUnique(Long merchantId, Integer type, String name, Long excludeId) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getMerchantId, merchantId)
                .eq(Category::getType, type)
                .eq(Category::getName, name);
        if (excludeId != null) {
            queryWrapper.ne(Category::getId, excludeId);
        }
        long count = count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(MessageConstant.CATEGORY_NAME_EXISTS);
        }
    }

    /**
     * 清除分类缓存
     * @param merchantId 商家ID
     * @param type 分类类型
     */
    private void clearCategoryCache(Long merchantId, Integer type) {
        String pattern = CATEGORY_CACHE_PREFIX + merchantId + "::*";
        Cursor<String> cursor = null;
        try {
            cursor = redisTemplate.scan(
                    ScanOptions.scanOptions()
                            .match(pattern)
                            .count(1000)
                            .build()
            );
            List<String> keysToDelete = new ArrayList<>();
            while (cursor.hasNext()) {
                String key = cursor.next();
                keysToDelete.add(key);
                // 【Redis缓存删除】
                if (keysToDelete.size() >= 1000) {
                    redisTemplate.delete(keysToDelete);
                    log.info("SCAN 删除分类缓存，batch size={}", keysToDelete.size());
                    keysToDelete.clear();
                }
            }
            // 删除剩余的
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("SCAN 删除分类缓存，final batch size={}", keysToDelete.size());
            }
        } catch (Exception e) {
            log.error("SCAN 删除分类缓存失败，merchantId={}, type={}", merchantId, type, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        }
    }