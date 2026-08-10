package com.zyj.productservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.context.AuthContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.BusinessException;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;
import com.zyj.productservice.mapper.DishFlavorMapper;
import com.zyj.productservice.mapper.DishMapper;
import com.zyj.productservice.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.service.impl
 * @Project：yun-shan
 * @name：DishServiceImpl
 * @Date：06 12月 2025  17:22
 * @Filename：DishServiceImpl
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {

    private final DishFlavorMapper dishFlavorMapper;
    private final DishMapper dishMapper;
    private final RedisTemplate redisTemplate;
    private final RedissonClient redissonClient;

    // 缓存key前缀常量
    private static final String DISH_CACHE_PREFIX = "dishCache:";
    private static final String SHOP_STATUS_PREFIX = "shop:status:"; // 商家状态缓存前缀
    private static final long DISH_CACHE_TTL = 30; // 缓存过期时间（分钟）

    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("添加菜品:{}",dishDTO);
        // 【权限校验】非超管强制设置merchantId为当前员工所属商家
        Long merchantId = resolveMerchantId(dishDTO.getMerchantId());
        dishDTO.setMerchantId(merchantId);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        save(dish);
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }

        // 【缓存清除】清除对应商家+分类的菜品缓存
        clearDishCache(merchantId, dishDTO.getCategoryId());
    }

    @Override
    @Transactional
    public void updateDish(DishDTO dishDTO) {
        log.info("修改菜品:{}",dishDTO);
        Long id = dishDTO.getId();
        Dish oldDish = dishMapper.getById(id);
        if (oldDish == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldDish.getMerchantId());

        // 【权限校验】非超管不能修改merchantId为自己之外的值
        Long newMerchantId = resolveMerchantId(dishDTO.getMerchantId());
        dishDTO.setMerchantId(newMerchantId);

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO,dish);

        // 删除旧口味
        List<Long> ids = Collections.singletonList(id);
        dishFlavorMapper.deleteFlavor(ids);

        // 添加新口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && flavors.size() > 0){
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(id));
            dishFlavorMapper.insertBatch(flavors);
        }

        dishMapper.update(dish);

        // 【缓存清除】清除旧分类和新分类的缓存
        clearDishCache(newMerchantId, oldDish.getCategoryId());
        if (!oldDish.getCategoryId().equals(dishDTO.getCategoryId())) {
            clearDishCache(newMerchantId, dishDTO.getCategoryId());
        }
    }

    @Override
    @Transactional
    public void deleteDish(List<Long> ids) {
        log.info("开始删除菜品:{}", ids);
        // 【权限校验】逐个校验每个菜品的商家归属
        List<Dish> dishes = listByIds(ids);
        for (Dish dish : dishes) {
            if (dish == null) continue;
            checkMerchantPermission(dish.getMerchantId());
        }

        removeByIds(ids);
        dishFlavorMapper.deleteFlavor(ids);

        // 【缓存清除】清除每个菜品的对应分类缓存
        for (Dish dish : dishes) {
            if (dish != null) {
                clearDishCache(dish.getMerchantId(), dish.getCategoryId());
            }
        }
    }

    @Override
    public DishVO getById(Long id) {
        log.info("查询菜品信息：{}",id);
        Dish dish = dishMapper.getById(id);
        if (dish == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        //checkMerchantPermission(dish.getMerchantId());

        List<DishFlavor> dishFlavor = dishFlavorMapper.getByIdFlavor(id);
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish,dishVO);
        dishVO.setFlavors(dishFlavor);
        log.info("菜品信息:{}",dishFlavor);
        return dishVO;
    }

    @Override
    public PageResult<DishVO> page(DishPageQueryDTO dishPageQueryDTO) {
        Page<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        QueryWrapper<Dish> queryWrapper = new QueryWrapper<>();
        if(dishPageQueryDTO.getName() != null){
            queryWrapper.like("name", dishPageQueryDTO.getName());
        }
        if(dishPageQueryDTO.getCategoryId() != null) {
            queryWrapper.eq("category_id", dishPageQueryDTO.getCategoryId());
        }
        if(dishPageQueryDTO.getStatus() != null){
            queryWrapper.eq("status", dishPageQueryDTO.getStatus());
        }
        // [TENANT-PLUGIN] applyMerchantFilter 已被 TenantLineInnerInterceptor 替代，测试通过后删除
        // applyMerchantFilter(queryWrapper);

        IPage<Dish> dishPage = dishMapper.selectPage(page, queryWrapper);
        List<DishVO> dishVOList = new ArrayList<>();
        for (Dish dish : dishPage.getRecords()) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish,dishVO);
            List<DishFlavor> dishFlavor = dishFlavorMapper.getByIdFlavor(dish.getId());
            dishVO.setFlavors(dishFlavor);
            dishVOList.add(dishVO);
        }
        return new PageResult<>(dishPage.getTotal(),dishVOList);
    }

    @Override
    public List<Dish> getByIdClassify(Long categoryId, Long merchantId) {
        log.info("根据分类id查询菜品：categoryId={}, merchantId={}", categoryId, merchantId);
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Dish::getCategoryId, categoryId);
        // [TENANT-PLUGIN] 非超管时 SQL 由 TenantLineInnerInterceptor 自动追加 merchant_id 条件，
        // 故此处仅当显式传入 merchantId 时才追加（用于 C端用户/Feign 场景）
        lambdaQueryWrapper.eq(merchantId != null, Dish::getMerchantId, merchantId);
        return dishMapper.selectList(lambdaQueryWrapper);
    }

    @Override
    public void startOrStop(Integer status,Long id) {
        log.info("开始修改菜品状态：{}",id);
        Dish oldDish = dishMapper.getById(id);
        if (oldDish == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldDish.getMerchantId());

        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        // 【缓存清除】清除对应缓存
        clearDishCache(oldDish.getMerchantId(), oldDish.getCategoryId());
    }

    /**
     * 根据分类id和商家ID查询菜品及口味（用户端，带Redis缓存）
     * key = dishCache::{merchantId}::{categoryId}
     */
    @Override
    public List<DishVO> listWithFlavor(Dish dish) {
        Long merchantId = dish.getMerchantId();
        Long categoryId = dish.getCategoryId();
        String cacheKey = DISH_CACHE_PREFIX + merchantId + "::" + categoryId;
        log.info("根据分类id查询菜品及口味（用户端），key={}", cacheKey);

        // 【Redis缓存读取】先从缓存获取
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("命中菜品缓存，key={}", cacheKey);
            return (List<DishVO>) cached;
        }

        // 【Redisson互斥锁】防止缓存击穿，同一商家+分类同时只有一个线程查库写缓存
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);  // 获取锁
        try {
            lock.lock(); // 获取锁
            // Double-check：获取锁后再次检查缓存
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("双重检查命中菜品缓存，key={}", cacheKey);
                return (List<DishVO>) cached;
            }

            // 缓存未命中，查数据库
            List<Dish> dishList = dishMapper.getByIdClassify(categoryId.toString(), merchantId);

            List<DishVO> dishVOList = new ArrayList<>();
            for (Dish d : dishList) {
                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(d, dishVO);
                List<DishFlavor> flavors = dishFlavorMapper.getByIdFlavor(d.getId());
                dishVO.setFlavors(flavors);
                dishVOList.add(dishVO);
            }

            // 【Redis缓存写入】
            redisTemplate.opsForValue().set(cacheKey, dishVOList, DISH_CACHE_TTL, TimeUnit.MINUTES);
            log.info("菜品缓存已写入，key={}, size={}", cacheKey, dishVOList.size());

            return dishVOList;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据关键字和商家ID查询菜品及口味（用户端）
     */
    @Override
    public List<DishVO> searchDish(String keyword, Long merchantId) {
        if (merchantId == null) {
            log.warn("商家ID不能为空");
            return List.of();
        }

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS_PREFIX + merchantId);
        if (shopStatus == null || shopStatus == 0) {
            log.warn("商家不存在或已停业，merchantId={}, status={}", merchantId, shopStatus);
            return List.of();
        }

        List<Dish> dishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getMerchantId, merchantId)
                        .eq(Dish::getStatus, 1)
                        .like(keyword != null && !keyword.isEmpty(), Dish::getName, keyword)
        );

        return dishes.stream().map(dish -> {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(dish, dishVO);
            List<DishFlavor> flavors = dishFlavorMapper.getByIdFlavor(dish.getId());
            dishVO.setFlavors(flavors);
            return dishVO;
        }).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Integer countStatusDish(Integer status) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Dish::getStatus, status);
        // [TENANT-PLUGIN] 以下手动 merchant_id 过滤已由 TenantLineInnerInterceptor 自动处理，测试通过后删除
        // if (!AuthContext.isSuperAdmin()) {
        //     wrapper.eq(Dish::getMerchantId, AuthContext.getCurrentMerchantId());
        // }
        return Math.toIntExact(count(wrapper));
    }

    // ==================== 私有方法 ====================

    /**
     * 校验商家归属权限
     * @param targetMerchantId 目标商家ID
     */
    private void checkMerchantPermission(Long targetMerchantId) {
        if (!AuthContext.isSuperAdmin()) {
            Long currentMerchantId = AuthContext.getCurrentMerchantId();
            if (currentMerchantId == null || !currentMerchantId.equals(targetMerchantId)) {
                throw new BusinessException(MessageConstant.MERCHANT_PERMISSION_DENIED);
            }
        }
    }

    /**
     * 解析merchantId：非超管强制使用当前员工merchantId
     * @param inputMerchantId 前端传入的merchantId
     * @return 解析后的merchantId
     */
    private Long resolveMerchantId(Long inputMerchantId) {
        if (!AuthContext.isSuperAdmin()) {
            return AuthContext.getCurrentMerchantId();
        }
        return inputMerchantId;
    }

    /**
     * 查询时自动添加merchantId过滤（非超管）
     * @param queryWrapper QueryWrapper
     */
    // [TENANT-PLUGIN] 此方法已被 TenantLineInnerInterceptor 替代，测试通过后删除整个方法
    // private void applyMerchantFilter(QueryWrapper<Dish> queryWrapper) {
    //     if (!AuthContext.isSuperAdmin()) {
    //         Long currentMerchantId = AuthContext.getCurrentMerchantId();
    //         if (currentMerchantId != null) {
    //             queryWrapper.eq("merchant_id", currentMerchantId);
    //         }
    //     } else {
    //         // 超管可指定merchantId过滤（通过DTO传入）
    //         // 已通过DTO的merchantId字段处理
    //     }
    // }

    /**
     * 清除菜品缓存
     * @param merchantId 商家ID
     * @param categoryId 分类ID
     */
    private void clearDishCache(Long merchantId, Long categoryId) {
        if (merchantId != null && categoryId != null) {
            String cacheKey = DISH_CACHE_PREFIX + merchantId + "::" + categoryId;
            Boolean deleted = redisTemplate.delete(cacheKey);
            log.info("清除菜品缓存，key={}, result={}", cacheKey, deleted);
        }
    }
}
