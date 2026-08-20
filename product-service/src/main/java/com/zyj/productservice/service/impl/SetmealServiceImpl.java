package com.zyj.productservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisStatusConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.AuthContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.BusinessException;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import com.zyj.productservice.config.RabbitMQConfig;
import com.zyj.productservice.mapper.DishMapper;
import com.zyj.productservice.mapper.SetmealDishMapper;
import com.zyj.productservice.mapper.SetmealMapper;
import com.zyj.productservice.service.SetmealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {

    private final SetmealMapper setmealMapper;
    private final SetmealDishMapper setmealDishMapper;
    private final DishMapper dishMapper;
    private final RedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    // 缓存key前缀常量
    private static final String SETMEAL_CACHE_PREFIX = "setmealCache:";
    private static final long SETMEAL_CACHE_TTL = 30; // 缓存过期时间（分钟）

    /**
     * 新增套餐（含权限校验+缓存清除）
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        // 【权限校验】非超管强制设置merchantId
        Long merchantId = resolveMerchantId(setmealDTO.getMerchantId());
        setmealDTO.setMerchantId(merchantId);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.insert(setmeal);

        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        setmealDishMapper.insertBatch(setmealDishes);

        // 【MQ缓存清除】事务提交后发送消息，异步清除套餐缓存
        afterCommit(() -> sendSetmealCacheClearMessage(merchantId, setmealDTO.getCategoryId()));
    }

    /**
     * 分页查询（含权限过滤）
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("查询套餐信息:{}", setmealPageQueryDTO);

        Page<SetmealVO> page = new Page<>(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(setmealPageQueryDTO.getName() != null, Setmeal::getName, setmealPageQueryDTO.getName())
                .eq(setmealPageQueryDTO.getCategoryId() != null, Setmeal::getCategoryId, setmealPageQueryDTO.getCategoryId())
                .eq(setmealPageQueryDTO.getStatus() != null, Setmeal::getStatus, setmealPageQueryDTO.getStatus());

        Page<SetmealVO> setmealIPage = setmealMapper.selectPageWithCategory(page, queryWrapper);
        return new PageResult<>(setmealIPage.getTotal(), setmealIPage.getRecords());
    }

    /**
     * 批量删除套餐（含权限校验+缓存清除）
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 【权限校验】逐个校验并检查状态
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if (setmeal == null) continue;
            checkMerchantPermission(setmeal.getMerchantId());
            if (StatusConstant.ENABLE == setmeal.getStatus()) {
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        for (Long setmealId : ids) {
            Setmeal setmeal = setmealMapper.getById(setmealId);
            if (setmeal != null) {
                setmealMapper.deleteById(setmealId);
                setmealDishMapper.deleteBySetmealId(setmealId);
            }
        }

        // 【MQ缓存清除】事务提交后发送消息，异步清除套餐缓存
        afterCommit(() -> {
            for (Long id : ids) {
                Setmeal setmeal = setmealMapper.getById(id);
                if (setmeal != null) {
                    sendSetmealCacheClearMessage(setmeal.getMerchantId(), setmeal.getCategoryId());
                }
            }
        });
    }

    /**
     * 根据id查询套餐和套餐菜品关系（含权限校验）
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        checkMerchantPermission(setmeal.getMerchantId());

        return setmealMapper.getByIdWithDish(id);
    }

    /**
     * 修改套餐（含权限校验+缓存清除）
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Long id = setmealDTO.getId();
        Setmeal oldSetmeal = setmealMapper.getById(id);
        if (oldSetmeal == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldSetmeal.getMerchantId());

        Long newMerchantId = resolveMerchantId(setmealDTO.getMerchantId());
        setmealDTO.setMerchantId(newMerchantId);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.update(setmeal);

        Long setmealId = setmealDTO.getId();
        setmealDishMapper.deleteBySetmealId(setmealId);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> setmealDish.setSetmealId(setmealId));
        setmealDishMapper.insertBatch(setmealDishes);

        // 【MQ缓存清除】事务提交后发送消息，异步清除套餐缓存
        afterCommit(() -> {
            sendSetmealCacheClearMessage(newMerchantId, oldSetmeal.getCategoryId());
            if (!oldSetmeal.getCategoryId().equals(setmealDTO.getCategoryId())) {
                sendSetmealCacheClearMessage(newMerchantId, setmealDTO.getCategoryId());
            }
        });
    }

    /**
     * 套餐起售、停售（含权限校验+缓存清除）
     */
    public void startOrStop(Integer status, Long id) {
        Setmeal oldSetmeal = setmealMapper.getById(id);
        if (oldSetmeal == null) {
            throw new BusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 【权限校验】非超管校验商家归属
        checkMerchantPermission(oldSetmeal.getMerchantId());

        if (status == StatusConstant.ENABLE) {
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if (dishList != null && dishList.size() > 0) {
                dishList.forEach(dish -> {
                    if (StatusConstant.DISABLE == dish.getStatus()) {
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

        // 【MQ缓存清除】事务提交后发送消息，异步清除套餐缓存
        afterCommit(() -> sendSetmealCacheClearMessage(oldSetmeal.getMerchantId(), oldSetmeal.getCategoryId()));
    }

    /**
     * 条件查询
     */
    public List<Setmeal> list(Setmeal setmeal) {
        return setmealMapper.list(setmeal);
    }

    /**
     * 根据套餐id查询菜品选项
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 用户端-查询套餐列表（带Redis缓存）
     * key = setmealCache::{merchantId}::{categoryId}
     */
    public List<SetmealVO> listWithCache(Long merchantId, Long categoryId) {
        String cacheKey = SETMEAL_CACHE_PREFIX + merchantId + "::" + categoryId;
        log.info("用户端查询套餐列表，key={}", cacheKey);

        // 【Redis缓存读取】
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("命中套餐缓存，key={}", cacheKey);
            return (List<SetmealVO>) cached;
        }

        // 【Redisson互斥锁】防止缓存击穿，同一商家+分类同时只有一个线程查库写缓存
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            // Double-check：获取锁后再次检查缓存
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("双重检查命中套餐缓存，key={}", cacheKey);
                return (List<SetmealVO>) cached;
            }

            // 缓存未命中，查数据库
            Setmeal query = new Setmeal();
            query.setMerchantId(merchantId);
            query.setCategoryId(categoryId);
            query.setStatus(StatusConstant.ENABLE);
            List<Setmeal> setmealList = setmealMapper.list(query);

            List<SetmealVO> voList = setmealList.stream().map(s -> {
                SetmealVO vo = new SetmealVO();
                BeanUtils.copyProperties(s, vo);
                return vo;
            }).collect(Collectors.toList());

            // 【Redis缓存写入】只缓存非空结果，避免空列表被缓存后一直命中空缓存
            if (voList != null && !voList.isEmpty()) {
                redisTemplate.opsForValue().set(cacheKey, voList, SETMEAL_CACHE_TTL, TimeUnit.MINUTES);
                log.info("套餐缓存已写入，key={}, size={}", cacheKey, voList.size());
            } else {
                log.info("套餐查询结果为空，不写缓存，key={}", cacheKey);
            }

            return voList;
        } finally {
            lock.unlock();
        }
    }

    public List<SetmealVO> searchSetmeal(String name, Long categoryId, Long merchantId) {
        if (merchantId == null) {
            log.warn("商家ID不能为空");
            return List.of();
        }

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(RedisStatusConstant.SHOP_STATUS_PREFIX + merchantId);
        if (shopStatus == null || shopStatus == 0) {
            log.warn("商家不存在或已停业，merchantId={}, status={}", merchantId, shopStatus);
            return List.of();
        }

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getMerchantId, merchantId)
                .eq(Setmeal::getStatus, StatusConstant.ENABLE)
                .like(name != null && !name.isEmpty(), Setmeal::getName, name)
                .eq(categoryId != null, Setmeal::getCategoryId, categoryId);

        List<Setmeal> setmeals = setmealMapper.selectList(queryWrapper);

        return setmeals.stream().map(setmeal -> {
            SetmealVO vo = new SetmealVO();
            BeanUtils.copyProperties(setmeal, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Integer countStatusSetmeal(Integer status) {
        LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Setmeal::getStatus, status);
        return Math.toIntExact(count(wrapper));
    }

    @Override
    public SetmealVO getByIdWithDishForCart(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        if (setmeal == null) {
            throw new BusinessException("套餐不存在");
        }
        return setmealMapper.getByIdWithDish(id);
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
     * 清除套餐缓存
     * @param merchantId 商家ID
     * @param categoryId 分类ID
     */
    private void clearSetmealCache(Long merchantId, Long categoryId) {
        if (merchantId == null) {
            return;
        }
        // 清除该商家全部套餐缓存（含 categoryId=null 时的 "::null" key，避免脏缓存滞留）
        String pattern = SETMEAL_CACHE_PREFIX + merchantId + "::*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("清除套餐缓存，pattern={}, count={}", pattern, keys.size());
        }
    }

    /**
     * 发送套餐缓存清除消息到 MQ（学习用：演示 MQ 异步删除缓存）
     * 消息格式：{ "merchantId": 123, "categoryId": 456 }
     */
    private void sendSetmealCacheClearMessage(Long merchantId, Long categoryId) {
        if (merchantId == null) {
            return;
        }
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("merchantId", merchantId);
            message.put("categoryId", categoryId);

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.SETMEAL_CACHE_EXCHANGE,
                RabbitMQConfig.SETMEAL_CLEAR_KEY,
                message
            );
            log.info("【MQ学习】已发送套餐缓存清除消息，merchantId={}, categoryId={}", merchantId, categoryId);
        } catch (Exception e) {
            log.error("【MQ学习】发送套餐缓存清除消息失败，merchantId={}, categoryId={}", merchantId, categoryId, e);
        }
    }

    /**
     * 事务提交后执行操作，避免并发读回填旧数据
     */
    private void afterCommit(Runnable task) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            }
        );
    }
}