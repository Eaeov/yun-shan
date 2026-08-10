package com.zyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantPageQueryDTO;
import com.sky.entity.Merchant;
import com.sky.result.PageResult;
import com.sky.vo.MerchantVO;

import java.util.Map;
import java.util.Set;

public interface MerchantService extends IService<Merchant> {

    /**
     * 分页查询商家（仅超管）
     */
    PageResult<MerchantVO> pageQuery(MerchantPageQueryDTO dto);

    /**
     * 查商家详情（超管/老板自己）
     */
    MerchantVO getById(Long id);

    /**
     * 新增商家（仅超管）
     */
    void save(MerchantDTO dto);

    /**
     * 修改商家（超管/老板自己）
     */
    void update(MerchantDTO dto);

    /**
     * 启用/停用（仅超管）
     */
    void updateStatus(Long id, Integer status);

    /**
     * C端-可用商家列表（status=1）
     */
    java.util.List<MerchantVO> listEnabled(String name);

    /**
     * C端-商家详情（status=1）
     */
    MerchantVO getEnabledById(Long id);

    /**
     * 根据ID查商家（Feign用）
     */
    MerchantVO getMerchantById(Long id);

    /**
     * 批量获取商家名映射（Feign用）
     */
    Map<Long, String> getMerchantNames(Set<Long> ids);

    /**
     * 校验商家有效（Feign用）
     */
    boolean isMerchantAvailable(Long id);

    Long getMerchantIdByUserId();
}
