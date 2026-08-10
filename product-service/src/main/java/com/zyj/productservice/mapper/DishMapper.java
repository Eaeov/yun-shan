package com.zyj.productservice.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Param;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import com.zyj.productservice.annotation.AutoFill;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);


    @AutoFill(value = OperationType.INSERT)
    int insert(Dish dish);
    //void insert(Dish dish);

    @AutoFill(value = OperationType.UPDATE)
    void update(Dish dish);

    void delectDish(List<Long> ids);

    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    List<DishVO> list(DishPageQueryDTO dishPageQueryDTO);

    @Select("select * from dish where category_id = #{categoryId} and merchant_id = #{merchantId}")
    List<Dish> getByIdClassify(@Param("categoryId") String categoryId, @Param("merchantId") Long merchantId);

    /**
     * 根据套餐id查询菜品
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId}")
    List<Dish> getBySetmealId(Long setmealId);

    @Select("select count(id) from dish where status = #{status}")
    Integer countStatusDish(Integer status);
}
