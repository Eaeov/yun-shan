package com.zyj.yunapi.client;

import com.sky.result.Result;
import com.sky.vo.MerchantVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 商家服务 Feign 接口
 * 提供给 product-service 等微服务调用
 * shop 营业状态相关请调用 cart-service
 */
@FeignClient("user-service")
public interface MerchantClient {

    /**
     * 根据ID查询商家信息
     */
    @GetMapping("/api/merchant/{id}")
    Result<MerchantVO> getMerchantById(@PathVariable Long id);

    /**
     * 批量获取商家名称  id → name
     */
    @GetMapping("/api/merchant/names")
    Result<Map<Long, String>> getMerchantNames(@RequestParam("ids") List<Long> ids);

    /**
     * 校验商家是否有效（存在且status=1）
     */
    @GetMapping("/api/merchant/{id}/available")
    Result<Boolean> isMerchantAvailable(@PathVariable Long id);
}
