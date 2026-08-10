package com.zyj.productservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系（含权限校验）
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 分页查询（含权限过滤）
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 批量删除套餐（含权限校验+缓存清除）
     */
    void deleteBatch(List<Long> ids);

    /**
     * 根据id查询套餐和关联的菜品数据（含权限校验）
     */
    SetmealVO getByIdWithDish(Long id);

    /**
     * 修改套餐（含权限校验+缓存清除）
     */
    void update(SetmealDTO setmealDTO);

    /**
     * 套餐起售、停售（含权限校验+缓存清除）
     */
    void startOrStop(Integer status, Long id);

    /**
     * 条件查询
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据id查询菜品选项
     */
    List<DishItemVO> getDishItemById(Long id);

    /**
     * 用户端-查询套餐列表（带Redis缓存）
     * key = setmealCache::{merchantId}::{categoryId}
     */
    List<SetmealVO> listWithCache(Long merchantId, Long categoryId);

    List<SetmealVO> searchSetmeal(String name, Long categoryId, Long merchantId);

    Integer countStatusSetmeal(Integer status);

    SetmealVO getByIdWithDishForCart(Long id);
}
