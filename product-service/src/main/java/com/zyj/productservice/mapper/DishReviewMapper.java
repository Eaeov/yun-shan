package com.zyj.productservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.DishReview;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 菜品评论 Mapper
 */
@Mapper
public interface DishReviewMapper extends BaseMapper<DishReview> {

    /**
     * 检查用户是否已对某订单的某菜品评论过
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param dishId 菜品ID
     * @return 已评论的数量
     */
    @Select("SELECT COUNT(1) FROM dish_review WHERE user_id = #{userId} AND order_id = #{orderId} AND dish_id = #{dishId} AND status = 1")
    int countByUserAndOrderAndDish(@Param("userId") Long userId,
                                   @Param("orderId") Long orderId,
                                   @Param("dishId") Long dishId);
}
