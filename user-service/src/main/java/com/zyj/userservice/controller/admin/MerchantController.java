package com.zyj.userservice.controller.admin;

import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.vo.MerchantVO;
import com.zyj.userservice.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端-商家管理
 */
@RestController("adminMerchantController")
@RequestMapping("/admin/merchant")
@Slf4j
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    /**
     * 分页查询商家列表（仅超管）
     */
    @GetMapping("/page")
    public Result<PageResult<MerchantVO>> page(MerchantPageQueryDTO dto) {
        log.info("商家分页查询：{}", dto);
        PageResult<MerchantVO> result = merchantService.pageQuery(dto);
        return Result.success(result);
    }

    /**
     * 根据ID查商家详情（超管/老板自己）
     */
    @GetMapping("/{id}")
    public Result<MerchantVO> getById(@PathVariable Long id) {
        log.info("查询商家详情：id={}", id);
        MerchantVO vo = merchantService.getById(id);
        return Result.success(vo);
    }

    /**
     * 新增商家（仅超管）
     */
    @PostMapping
    public Result<String> save(@RequestBody MerchantDTO dto) {
        log.info("新增商家：{}", dto);
        merchantService.save(dto);
        return Result.success();
    }

    /**
     * 修改商家信息（超管/老板自己）
     */
    @PutMapping
    public Result<String> update(@RequestBody MerchantDTO dto) {
        log.info("修改商家：{}", dto);
        merchantService.update(dto);
        return Result.success();
    }

    /**
     * 启用/停用商家（仅超管）
     */
    @PutMapping("/status/{status}/{id}")
    public Result<String> updateStatus(@PathVariable Integer status, @PathVariable Long id) {
        log.info("修改商家状态：id={}, status={}", id, status);
        merchantService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 根据用户ID获得商家ID
     */
    @GetMapping("/merchant/id")
    public Result<Long> getMerchantIdByUserId() {
        Long merchantId = merchantService.getMerchantIdByUserId();
        return Result.success(merchantId);
    }
}
