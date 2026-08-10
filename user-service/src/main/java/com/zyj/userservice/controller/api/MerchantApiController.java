package com.zyj.userservice.controller.api;

import com.sky.result.Result;
import com.sky.vo.MerchantVO;
import com.zyj.userservice.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Feign 远程调用接口 - 商家
 * 提供给其他微服务调用，不走网关鉴权
 */
@RestController
@RequestMapping("/api/merchant")
@Slf4j
@RequiredArgsConstructor
public class MerchantApiController {

    private final MerchantService merchantService;

    /**
     * 根据ID查询商家信息
     */
    @GetMapping("/{id}")
    public Result<MerchantVO> getMerchantById(@PathVariable Long id) {
        MerchantVO vo = merchantService.getMerchantById(id);
        return Result.success(vo);
    }

    /**
     * 批量获取商家名称映射
     */
    @GetMapping("/names")
    public Result<Map<Long, String>> getMerchantNames(@RequestParam("ids") List<Long> ids) {
        Map<Long, String> map = merchantService.getMerchantNames(
                new java.util.HashSet<>(ids != null ? ids : Collections.emptyList()));
        return Result.success(map);
    }

    /**
     * 校验商家是否有效（存在且status=1）
     */
    @GetMapping("/{id}/available")
    public Result<Boolean> isMerchantAvailable(@PathVariable Long id) {
        boolean available = merchantService.isMerchantAvailable(id);
        return Result.success(available);
    }
}
