package com.zyj.productservice.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.DishVO;
import com.zyj.productservice.service.DishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.admin
 * @Project：yun-shan
 * @name：DishController
 * @Date：06 12月 2025  17:23
 * @Filename：DishController
 * 管理端-菜品管理（含多商家隔离）
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/dish")
public class DishController {

    private final DishService dishService;

    /**
     * 新增菜品（Service层处理权限校验+缓存清除）
     * @param dishDTO
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    /**
     * 修改菜品（Service层校验商家归属+缓存清除）
     * @param dishDTO
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品信息:{}", dishDTO);
        dishService.updateDish(dishDTO);
        return Result.success();
    }

    /**
     * 批量删除菜品（Service层校验商家归属+缓存清除）
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> delete(@RequestParam("ids") List<Long> ids) {
        log.info("删除菜品:{}", ids);
        dishService.deleteDish(ids);
        return Result.success();
    }

    /**
     * 根据ID查询菜品（Service层校验商家归属）
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("查询菜品:{}", id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }

    /**
     * 分页查询菜品（Service层自动过滤merchantId）
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult<DishVO>> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult<DishVO> pageResult = dishService.page(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 根据分类ID和商家ID查询菜品列表
     * @param categoryId 分类ID
     * @param merchantId 商家ID（超管可选，商家老板强制为本商家）
     * @return
     */
    @GetMapping("/list")
    public Result<List<Dish>> getByIdClassify(
            @RequestParam Long categoryId,
            @RequestParam(required = false) Long merchantId) {
        log.info("根据分类id查询菜品：categoryId={}, merchantId={}", categoryId, merchantId);
        // [TENANT-PLUGIN] merchant_id 过滤已由 TenantLineInnerInterceptor 自动处理，
        // 非超管 SQL 层自动追加 WHERE merchant_id = ?，测试通过后删除下方注释代码
        // if (merchantId == null) {
        //     merchantId = com.sky.context.AuthContext.getCurrentMerchantId();
        //     if (merchantId == null) {
        //         return Result.success(List.of());
        //     }
        // }
        List<Dish> list = dishService.getByIdClassify(categoryId, merchantId);
        return Result.success(list);
    }

    /**
     * 菜品起售/停售（Service层校验商家归属+缓存清除）
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 统计菜品状态
     */
    @GetMapping("/status")
    public Result<Integer> countStatusDish(Integer status) {
        return Result.success(dishService.countStatusDish(status));
    }

}
