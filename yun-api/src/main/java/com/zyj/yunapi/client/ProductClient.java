package com.zyj.yunapi.client;

import com.sky.result.Result;
import com.sky.vo.DishVO;
import com.sky.vo.SetmealVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.yunapi.client
 * @Project：yun-shan
 * @name：ProductClient
 * @Date：10 3月 2026  16:33
 * @Filename：ProductClient
 */
@FeignClient("product-service")
public interface ProductClient {
    /**
     * 根据id查询菜品
     * @param id
     * @return
     * 来自 DishController
     */
    @GetMapping("/admin/dish/{id}")
    Result<DishVO> getDishById(@PathVariable("id") Long id);

    /**
     * 根据id查询套餐
     * @param id
     * @return
     * 来自 SetmealController
     */
    @GetMapping("/admin/setmeal/{id}")
    Result<SetmealVO> getSetmealById(@PathVariable("id") Long id);

    /**
     * 统计菜品状态
     */
    @GetMapping("/status")
    Result<Integer> countStatusDish(Integer status) ;
    /**
     * 统计菜品状态
     */
    @GetMapping("/status")
    Result<Integer> countStatusSetmeal(Integer status) ;


    //模糊查询菜品
    @GetMapping("/user/dish/search")
    Result<List<DishVO>> search(@RequestParam String keyword, @RequestParam Long merchantId);
    //模糊查询套餐
    @GetMapping("/user/setmeal/search")
    Result<List<SetmealVO>> searchSetmeal(@RequestParam String keyword, @RequestParam Long merchantId);
}
