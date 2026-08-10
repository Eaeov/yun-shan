package com.zyj.productservice.controller.admin;

import com.sky.dto.DishReviewPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishReviewVO;
import com.zyj.productservice.service.DishReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商家端-菜品评论管理
 * 多商家隔离：非超管只能查看和回复自己商家菜品的评论
 *
 * @Author：zyj
 * @Date：29 4月 2026
 */
@Tag(name = "商家端-菜品评论管理")
@Slf4j
@RequiredArgsConstructor
@RestController("adminDishReviewController")
@RequestMapping("/admin/dish-review")
public class DishReviewController {

    private final DishReviewService dishReviewService;

    /**
     * 分页查询本商家所有评论
     * 权限：非超管自动按当前员工 merchantId 过滤
     * @param dto 分页参数（merchantId 超管可选，非超管由后端注入）
     * @return
     */
    @Operation(summary = "查看本商家所有评论")
    @GetMapping("/page")
    public Result<PageResult<DishReviewVO>> page(DishReviewPageQueryDTO dto) {
        log.info("商家端查询评论，merchantId={}, dishId={}", dto.getMerchantId(), dto.getDishId());
        PageResult<DishReviewVO> pageResult = dishReviewService.pageAdminReviews(dto);
        return Result.success(pageResult);
    }

    /**
     * 回复/修改回复评论
     * 权限：评论对应的菜品 merchantId 必须等于当前员工的 merchantId
     * @param id 评论ID
     * @param body 请求体，含 replyContent
     * @return
     */
    @Operation(summary = "回复/修改回复评论")
    @PutMapping("/{id}/reply")
    public Result<String> reply(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String replyContent = body.get("replyContent");
        log.info("商家回复评论: id={}, content={}", id, replyContent);
        dishReviewService.replyReview(id, replyContent);
        return Result.success();
    }
}
