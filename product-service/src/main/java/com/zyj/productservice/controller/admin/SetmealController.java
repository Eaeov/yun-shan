package com.zyj.productservice.controller.admin;


import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.SetmealVO;
import com.zyj.productservice.service.SetmealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.admin
 * @Project：yun-shan
 * @name：SetmealController
 * @Date：07 12月 2025  17:19
 * @Filename：SetmealController
 * 管理端-套餐管理（含多商家隔离）
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/setmeal")
public class SetmealController {

    private final SetmealService setmealService;

    /**
     * 新增套餐（Service层处理权限校验+缓存清除）
     * @param setmealDTO
     * @return
     */
    @PostMapping
    public Result<String> save(@RequestBody SetmealDTO setmealDTO) {
        log.info("新增套餐:{}", setmealDTO);
        setmealService.saveWithDish(setmealDTO);
        return Result.success();
    }

    /**
     * 修改套餐（Service层校验商家归属+缓存清除）
     * @param setmealDTO
     * @return
     */
    @PutMapping
    public Result<String> update(@RequestBody SetmealDTO setmealDTO) {
        log.info("修改套餐:{}", setmealDTO);
        setmealService.update(setmealDTO);
        return Result.success();
    }

    /**
     * 批量删除套餐（Service层校验商家归属+缓存清除）
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result<String> delete(@RequestParam List<Long> ids) {
        log.info("批量删除套餐:{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 根据ID查询套餐（Service层校验商家归属）
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id) {
        log.info("根据id查询套餐:{}", id);
        SetmealVO setmealVO = setmealService.getByIdWithDishForCart(id);
        return Result.success(setmealVO);
    }
    //SetmealVO setmealVO = setmealService.getByIdWithDish(id);

    /**
     * 分页查询套餐（Service层自动过滤merchantId）
     * @param setmealPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO) {
        log.info("套餐分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 套餐起售/停售（Service层校验商家归属+缓存清除）
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        log.info("起售或停售套餐:{}", id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

    /**
     * 统计菜品状态
     */
    @GetMapping("/status")
    public Result<Integer> countStatusSetmeal(Integer status) {
        return Result.success(setmealService.countStatusSetmeal(status));
    }
}
