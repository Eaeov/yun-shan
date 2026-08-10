package com.zyj.productservice.controller.user;

import com.sky.dto.DishReviewPageQueryDTO;
import com.sky.dto.DishReviewSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishReviewVO;
import com.zyj.productservice.service.DishReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端-菜品评论
 *
 * @Author：zyj
 * @Date：29 4月 2026
 */
@Tag(name = "用户端-菜品评论")
@Slf4j
@RequiredArgsConstructor
@RestController("userDishReviewController")
@RequestMapping("/user/dish-review")
public class DishReviewController {

    private final DishReviewService dishReviewService;

    /**
     * 提交评论
     * 校验：登录、订单已完成且属于当前用户且包含该菜品、未重复评论
     * @param dto 评论参数
     * @return
     */
    @Operation(summary = "提交菜品评论")
    @PostMapping
    public Result<String> submit(@RequestBody DishReviewSubmitDTO dto) {
        log.info("用户提交评论: dishId={}, orderId={}", dto.getDishId(), dto.getOrderId());
        dishReviewService.submitReview(dto);
        return Result.success();
    }

    /**
     * 删除自己的评论（逻辑删除）
     * 校验：评论的 user_id 等于当前用户
     * @param id 评论ID
     * @return
     */
    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        log.info("用户删除评论: id={}", id);
        dishReviewService.deleteReview(id);
        return Result.success();
    }

    /**
     * 分页查询菜品评论列表
     * 只返回 status=1 的评论；匿名用户显示"匿名用户"
     * @param dto 分页参数（含 dishId）
     * @return
     */
    @Operation(summary = "分页查询菜品评论")
    @GetMapping("/page")
    public Result<PageResult<DishReviewVO>> page(DishReviewPageQueryDTO dto) {
        log.info("用户端查询评论: dishId={}, page={}", dto.getDishId(), dto.getPage());
        PageResult<DishReviewVO> pageResult = dishReviewService.pageUserReviews(dto);
        return Result.success(pageResult);
    }
}
