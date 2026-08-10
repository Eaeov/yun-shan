package com.zyj.productservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.service
 * @Project：yun-shan
 * @name：DishService
 * @Date：06 12月 2025  17:22
 * @Filename：DishService
 */
public interface DishService extends IService<Dish> {
    /**
     * 新增菜品同时保存对应的口味数据
     * @param dishDTO
     */
    void saveWithFlavor(DishDTO dishDTO);

    /**
     * 修改菜品同时修改对应的口味数据（含权限校验）
     * @param dishDTO
     */
    void updateDish(DishDTO dishDTO);

    /**
     * 删除菜品同时删除对应的口味数据（含权限校验）
     * @param ids
     */
    void deleteDish(List<Long> ids);

    /**
     * 根据id查询菜品及其对应的口味数据（含权限校验）
     * @param id
     * @return
     */
    DishVO getById(Long id);

    /**
     * 分页查询菜品数据（含权限过滤）
     * @param dishPageQueryDTO
     * @return
     */
    PageResult<DishVO> page(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据分类id和商家ID查询菜品数据
     * @param categoryId 分类ID
     * @param merchantId 商家ID
     * @return
     */
    List<Dish> getByIdClassify(Long categoryId, Long merchantId);

    /**
     * 启用或禁用菜品（含权限校验）
     * @param status
     * @param id
     */
    void startOrStop(Integer status,Long id);

    /**
     * 根据条件查询菜品及口味（用户端，带Redis缓存）
     * @param dish 查询条件（含merchantId和categoryId）
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    List<DishVO> searchDish(String keyword, Long merchantId);

    Integer countStatusDish(Integer status);
}
