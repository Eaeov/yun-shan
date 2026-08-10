package com.zyj.productservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 新增分类（含权限校验+联合唯一校验+缓存清除）
     */
    void save(CategoryDTO categoryDTO);

    /**
     * 分页查询（含权限过滤）
     */
    PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类（含权限校验+缓存清除）
     */
    void deleteById(Long id);

    /**
     * 修改分类（含权限校验+联合唯一校验+缓存清除）
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 启用、禁用分类（含权限校验+缓存清除）
     */
    void startOrStop(Integer status, Long id);

    /**
     * 根据类型查询分类（admin端，含权限过滤）
     */
    List<Category> list(Integer type);

    /**
     * 用户端-查询分类列表（带Redis缓存）
     * key = categoryCache::{merchantId}::{type}
     */
    List<Category> listWithCache(Long merchantId, Integer type);
}
