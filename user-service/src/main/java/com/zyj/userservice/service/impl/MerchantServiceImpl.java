package com.zyj.userservice.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.StatusConstant;
import com.sky.context.AuthContext;
import com.sky.context.BaseContext;
import com.sky.dto.MerchantDTO;
import com.sky.dto.MerchantPageQueryDTO;
import com.sky.entity.Employee;
import com.sky.entity.Merchant;
import com.sky.exception.BusinessException;
import com.sky.result.PageResult;
import com.sky.vo.MerchantVO;
import com.zyj.userservice.mapper.EmployeeMapper;
import com.zyj.userservice.mapper.MerchantMapper;
import com.zyj.userservice.service.MerchantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.beancontext.BeanContext;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    private final MerchantMapper merchantMapper;
    private final EmployeeMapper employeeMapper;
    private final RedisTemplate redisTemplate;

    private static final String MERCHANT_INFO_PREFIX = "merchant:info:";
    private static final String MERCHANT_LIST_KEY = "merchant:list";
    private static final long MERCHANT_INFO_TTL = 30;
    private static final long MERCHANT_LIST_TTL = 5;

    @Override
    public PageResult<MerchantVO> pageQuery(MerchantPageQueryDTO dto) {
        checkSuperAdmin();
        log.info("超管分页查询商家，name={}, status={}", dto.getName(), dto.getStatus());

        Page<Merchant> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<Merchant> qw = new LambdaQueryWrapper<>();
        qw.like(dto.getName() != null && !dto.getName().isEmpty(), Merchant::getName, dto.getName());
        qw.eq(dto.getStatus() != null, Merchant::getStatus, dto.getStatus());
        qw.orderByDesc(Merchant::getCreateTime);

        Page<Merchant> p = merchantMapper.selectPage(page, qw);
        List<MerchantVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), vos);
    }

    @Override
    public MerchantVO getById(Long id) {
        Employee emp = getEmp();
        Merchant merchant = merchantMapper.selectById(id);
        if (merchant == null) throw new BusinessException("商家不存在");

        // 【权限】超管可查任意；老板只能查自己
        if (emp.getRole() != 0 && !emp.getMerchantId().equals(id)) {
            throw new BusinessException("无权查看");
        }
        return toVO(merchant);
    }

    @Override
    @Transactional
    public void save(MerchantDTO dto) {
        checkSuperAdmin();
        log.info("超管新增商家：{}", dto.getName());
        Merchant m = new Merchant();
        BeanUtils.copyProperties(dto, m);
        m.setId(null);
        m.setStatus(StatusConstant.ENABLE);
        m.setCreateTime(java.time.LocalDateTime.now());
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.insert(m);
        redisTemplate.delete(MERCHANT_LIST_KEY);
    }

    @Override
    @Transactional
    public void update(MerchantDTO dto) {
        Employee emp = getEmp();
        Merchant old = merchantMapper.selectById(dto.getId());
        if (old == null) throw new BusinessException("商家不存在");

        // 【权限】超管可改任意；老板只能改自己的
        if (emp.getRole() != 0 && !emp.getMerchantId().equals(dto.getId())) {
            throw new BusinessException("无权修改");
        }

        Merchant m = new Merchant();
        BeanUtils.copyProperties(dto, m);
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.updateById(m);

        // 【缓存清除】
        redisTemplate.delete(MERCHANT_INFO_PREFIX + dto.getId());
        redisTemplate.delete(MERCHANT_LIST_KEY);
        log.info("商家[{}]缓存已清除", dto.getId());
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        checkSuperAdmin();
        log.info("超管修改商家[{}]状态为：{}", id, status);
        Merchant m = new Merchant();
        m.setId(id);
        m.setStatus(status);
        m.setUpdateTime(java.time.LocalDateTime.now());
        merchantMapper.updateById(m);
        redisTemplate.delete(MERCHANT_INFO_PREFIX + id);
        redisTemplate.delete(MERCHANT_LIST_KEY);

    }

    @Override
    public List<MerchantVO> listEnabled(String name) {
        // 【缓存读取】先读全量列表缓存
        Object cached = redisTemplate.opsForValue().get(MERCHANT_LIST_KEY);
        if (cached != null) {
            @SuppressWarnings("unchecked")
            List<MerchantVO> list = (List<MerchantVO>) cached;
            if (name != null && !name.isEmpty()) {
                return list.stream().filter(v -> v.getName().contains(name)).collect(Collectors.toList());
            }
            return list;
        }

        LambdaQueryWrapper<Merchant> qw = new LambdaQueryWrapper<>();
        qw.eq(Merchant::getStatus, StatusConstant.ENABLE).orderByDesc(Merchant::getCreateTime);
        List<Merchant> merchants = merchantMapper.selectList(qw);
        List<MerchantVO> vos = merchants.stream().map(this::toVO).collect(Collectors.toList());

        redisTemplate.opsForValue().set(MERCHANT_LIST_KEY, vos, MERCHANT_LIST_TTL, TimeUnit.MINUTES);
        log.info("商家列表缓存已写入，size={}", vos.size());

        if (name != null && !name.isEmpty()) {
            return vos.stream().filter(v -> v.getName().contains(name)).collect(Collectors.toList());
        }
        return vos;
    }

    @Override
    public MerchantVO getEnabledById(Long id) {
        Merchant m = merchantMapper.selectById(id);
        if (m == null || !m.getStatus().equals(StatusConstant.ENABLE)) {
            throw new BusinessException("商家不存在或已停用");
        }
        return toVO(m);
    }

    @Override
    public MerchantVO getMerchantById(Long id) {
        // 【Feign用，走info缓存】
        String key = MERCHANT_INFO_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return JSON.parseObject(cached.toString(), MerchantVO.class);
        }
        Merchant m = merchantMapper.selectById(id);
        if (m == null) return null;
        MerchantVO vo = toVO(m);
        redisTemplate.opsForValue().set(key, JSON.toJSONString(vo), MERCHANT_INFO_TTL, TimeUnit.MINUTES);
        return vo;
    }

    @Override
    public Map<Long, String> getMerchantNames(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        // 先查缓存
        Map<Long, String> result = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        for (Long id : ids) {
            Object cached = redisTemplate.opsForValue().get(MERCHANT_INFO_PREFIX + id);
            if (cached != null) {
                MerchantVO vo = JSON.parseObject(cached.toString(), MerchantVO.class);
                result.put(id, vo.getName());
            } else {
                missIds.add(id);
            }
        }
        // 未命中查库
        if (!missIds.isEmpty()) {
            List<Merchant> merchants = merchantMapper.selectBatchIds(missIds);
            for (Merchant m : merchants) {
                result.put(m.getId(), m.getName());
                MerchantVO vo = toVO(m);
                redisTemplate.opsForValue().set(MERCHANT_INFO_PREFIX + m.getId(), JSON.toJSONString(vo), MERCHANT_INFO_TTL, TimeUnit.MINUTES);
            }
        }
        return result;
    }

    @Override
    public boolean isMerchantAvailable(Long id) {
        MerchantVO vo = getMerchantById(id);
        return vo != null && vo.getStatus().equals(StatusConstant.ENABLE);
    }

    @Override
    public Long getMerchantIdByUserId() {
        Long userId = BaseContext.getCurrentId();
        Employee byId = employeeMapper.getById(userId);
        if (byId != null) {
            return byId.getMerchantId();
        }
        return null;
    }

    // ==================== 私有 ====================
    private void checkSuperAdmin() {
        Employee emp = getEmp();
        if (emp.getRole() == null || emp.getRole() != 0) {
            throw new BusinessException("仅超级管理员可操作");
        }
    }

    private Employee getEmp() {
        Employee emp = AuthContext.getCurrentEmployee();
        if (emp == null) throw new BusinessException("未登录");
        return emp;
    }

    private MerchantVO toVO(Merchant m) {
        return MerchantVO.builder()
                .id(m.getId()).name(m.getName()).logo(m.getLogo())
                .description(m.getDescription()).status(m.getStatus())
                .createTime(m.getCreateTime()).build();
    }
}
