package com.zyj.productservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.mapper
 * @Project：yun-shan
 * @name：DishFlavorMapper
 * @Date：06 12月 2025  17:50
 * @Filename：DishFlavorMapper
 */
@Mapper
public interface DishFlavorMapper extends BaseMapper<DishFlavor> {
    void insertBatch(List<DishFlavor> flavors);
    @Select("select * from dish_flavor where dish_id = #{id}")
    List<DishFlavor> getByIdFlavor(Long id);

    void deleteFlavor(List<Long> ids);
}
