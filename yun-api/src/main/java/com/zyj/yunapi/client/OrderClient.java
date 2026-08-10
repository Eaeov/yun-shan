package com.zyj.yunapi.client;

import com.sky.dto.GoodsSalesDTO;
import com.sky.result.Result;
import com.sky.vo.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @Author：zyj
 * @Package：com.zyj.yunapi.client
 * @Project：yun-shan
 * @name：OrderClient
 * @Date：10 3 月 2026  16:33
 * @Filename：OrderClient
 */
@FeignClient("order-service")
public interface OrderClient {
    /**
     * 根据条件统计订单金额
     * @param map 查询条件（包含 beginTime, endTime 等）
     * @return 营业额
     */
    @PostMapping("/admin/order/sumByMap")
   Result<Double> sumByMap(@RequestBody Map<String, Object> map);

    /**
     * 根据条件统计订单数量
     * @param map 查询条件
     * @return 订单数量
     */
    @PostMapping("/admin/order/countByMap")
   Result<Integer> countByMap(@RequestBody Map<String, Object> map);

    /**
     * 获取销量前 10 的商品
     * @param begin 开始日期
     * @param end 结束日期
     * @return 商品销量列表
     */
    @GetMapping("/admin/order/getSalesTop10")
   Result<List<GoodsSalesDTO>> getSalesTop10(
            @RequestParam String begin,
            @RequestParam String end
    );

    /**
     * 查询订单详情（用户端，用于评论校验）
     * @param id 订单ID
     * @return 订单详情
     */
    @GetMapping("/user/order/orderDetail/{id}")
    Result<OrderVO> getOrderDetail(@PathVariable Long id);
}
