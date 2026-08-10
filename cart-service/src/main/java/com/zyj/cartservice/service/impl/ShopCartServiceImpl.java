package com.zyj.cartservice.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;

import com.zyj.cartservice.mapper.ShopCartMapper;
import com.zyj.cartservice.service.ShopCartService;
import com.zyj.yunapi.client.ProductClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车业务层实现类
 * 处理购物车的添加、查询、清空、商品数量减少等核心业务逻辑
 *
 * @Author：zyj
 * @Package：com.zyj.cartservice.service.impl
 * @Project：yun-shan
 * @name：ShopCartServiceImpl
 * @Date：11 12月 2025  15:23
 * @Filename：ShopCartServiceImpl
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ShopCartServiceImpl implements ShopCartService {
    private final ShopCartMapper shopCartMapper;
    private final ProductClient productClient;
    private final RedisTemplate redisTemplate;

    private static final String SHOP_STATUS_PREFIX = "shop:status:";

    @Override
    public void addShopCart(ShoppingCartDTO shoppingCartDTO) {
        log.info("添加商品到购物车，参数：{}", shoppingCartDTO);

        if (shoppingCartDTO.getMerchantId() == null) {
            log.error("添加购物车失败：商家ID不能为空");
            throw new ShoppingCartBusinessException(MessageConstant.MERCHANT_ID_CAN_NOT_BE_EMPTY);
        }

        Long userId = BaseContext.getCurrentId();

        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(SHOP_STATUS_PREFIX + shoppingCartDTO.getMerchantId());
        if (shopStatus == null || shopStatus != 1) {
            log.error("添加购物车失败：商家已打烊，merchantId={}", shoppingCartDTO.getMerchantId());
            throw new ShoppingCartBusinessException("商家已打烊，无法添加商品");
        }

        ShoppingCart queryCart = new ShoppingCart();
        queryCart.setUserId(userId);
        List<ShoppingCart> existingItems = shopCartMapper.list(queryCart);
        if (!existingItems.isEmpty()) {
            Long existingMerchantId = existingItems.get(0).getMerchantId();
            if (!existingMerchantId.equals(shoppingCartDTO.getMerchantId())) {
                log.error("添加购物车失败：购物车已有其他商家商品，existingMerchantId={}，newMerchantId={}",
                        existingMerchantId, shoppingCartDTO.getMerchantId());
                throw new ShoppingCartBusinessException("购物车已有其他商家商品，请先清空购物车");
            }
        }

        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, cart);
        cart.setUserId(userId);
        List<ShoppingCart> list = shopCartMapper.list(cart);

        if (list != null && list.size() > 0) {
            ShoppingCart cart1 = list.get(0);
            cart1.setNumber(cart1.getNumber() + 1);
            shopCartMapper.updateNumberById(cart1);
        } else {
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                DishVO dishVO = productClient.getDishById(dishId).getData();
                cart.setName(dishVO.getName());
                cart.setImage(dishVO.getImage());
                cart.setAmount(dishVO.getPrice());
            } else {
                Long setmealId = shoppingCartDTO.getSetmealId();
                SetmealVO setmealVO = productClient.getSetmealById(setmealId).getData();
                cart.setName(setmealVO.getName());
                cart.setImage(setmealVO.getImage());
                cart.setAmount(setmealVO.getPrice());
            }
            cart.setCreateTime(LocalDateTime.now());
            cart.setNumber(1);
            shopCartMapper.insertShopCart(cart);
        }
    }

    @Override
    public List<ShoppingCart> list() {
        log.info("查询当前登录用户的购物车列表");
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        return shopCartMapper.list(shoppingCart);
    }

    @Override
    public List<ShoppingCart> list(Long merchantId) {
        log.info("查询当前登录用户指定商家的购物车列表，merchantId={}", merchantId);
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        shoppingCart.setMerchantId(merchantId);
        return shopCartMapper.list(shoppingCart);
    }

    @Override
    public void delete() {
        log.info("清空当前登录用户的购物车");
        Long userId = BaseContext.getCurrentId();
        shopCartMapper.delete(userId);
    }

    @Override
    public void delete(Long merchantId) {
        log.info("清空当前登录用户指定商家的购物车，merchantId={}", merchantId);
        Long userId = BaseContext.getCurrentId();
        shopCartMapper.deleteByUserIdAndMerchantId(userId, merchantId);
    }

    @Override
    public void sub(ShoppingCartDTO shoppingCartDTO) {
        log.info("减少购物车商品数量，参数：{}", shoppingCartDTO);

        if (shoppingCartDTO.getMerchantId() == null) {
            log.error("减少购物车失败：商家ID不能为空");
            throw new ShoppingCartBusinessException(MessageConstant.MERCHANT_ID_CAN_NOT_BE_EMPTY);
        }

        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        List<ShoppingCart> list = shopCartMapper.list(shoppingCart);
        if (list != null && list.size() > 0) {
            ShoppingCart cart = list.get(0);

            if (cart.getNumber() == 1) {
                shopCartMapper.deleteById(cart.getId());
            } else {
                cart.setNumber(cart.getNumber() - 1);
                shopCartMapper.updateNumberById(cart);
            }
        }
    }
}
