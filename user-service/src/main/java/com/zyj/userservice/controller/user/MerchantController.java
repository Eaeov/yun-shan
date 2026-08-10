package com.zyj.userservice.controller.user;

import com.sky.result.Result;
import com.sky.vo.MerchantVO;
import com.zyj.userservice.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端-商家相关接口（无需登录）
 */
@RestController("userMerchantController")
@RequestMapping("/user/merchant")
@Slf4j
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 获取可用商家列表（status=1），可模糊搜索
     */
    @GetMapping("/list")
    public Result<List<MerchantVO>> list(@RequestParam(required = false) String name) {
        log.info("用户端查询商家列表：name={}", name);
        List<MerchantVO> list = merchantService.listEnabled(name);
        return Result.success(list);
    }

    /**
     * 获取商家详情（status=1才返回）
     */
    @GetMapping("/{id}")
    public Result<MerchantVO> getById(@PathVariable Long id) {
        log.info("用户端查询商家详情：id={}", id);
        MerchantVO vo = merchantService.getEnabledById(id);
        return Result.success(vo);
    }
}
