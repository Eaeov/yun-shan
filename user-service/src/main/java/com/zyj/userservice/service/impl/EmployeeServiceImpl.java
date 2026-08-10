package com.zyj.userservice.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.AuthContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.BusinessException;
import com.sky.exception.PasswordErrorException;
import com.sky.result.PageResult;
import com.zyj.userservice.mapper.EmployeeMapper;
import com.zyj.userservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 员工管理服务实现（多商家权限）
 * role: 0=超级管理员（merchantId=null）、1=商家老板、2=普通员工
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    private final EmployeeMapper employeeMapper;

    /**
     * 分页查询员工（超管可按商家ID查询，老板只能查询自己商家的员工）
     * @param dto
     * @return
     */
    @Override
    public PageResult<Employee> page(EmployeePageQueryDTO dto) {
        // [TENANT-PLUGIN] resolveMerchantId + .eq(merchantId) 已由 TenantLineInnerInterceptor 自动处理，
        // 超管仍可通过 dto.getMerchantId() 主动过滤，测试通过后一并删除下面两行注释
        // Employee emp = getEmp();
        // Long merchantId = resolveMerchantId(emp, dto.getMerchantId());

        Page<Employee> page = new Page<>(dto.getPage(), dto.getPageSize());
        LambdaQueryWrapper<Employee> qw = new LambdaQueryWrapper<>();
        qw.eq(dto.getName() != null && !dto.getName().isEmpty(), Employee::getName, dto.getName());
        // [TENANT-PLUGIN] .eq(merchantId) 自动过滤已由插件处理，测试通过后删除下行
        // qw.eq(merchantId != null, Employee::getMerchantId, merchantId);
        qw.orderByDesc(Employee::getCreateTime);

        IPage<Employee> iPage = employeeMapper.selectPage(page, qw);
        return new PageResult<>(iPage.getTotal(), iPage.getRecords());
    }

    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        Employee employee = employeeMapper.getByUsername(username);

        if (employee == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        if (!BCrypt.checkpw(password, employee.getPassword())) {  // 盐值
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        return employee;
    }

    @Override
    public void addEmployees(EmployeeDTO dto) {
        Employee emp = getEmp();

        if (emp.getRole() != null && emp.getRole() == 0) {
            // 【权限】超管：可新增任意角色、任意商家
            log.info("超管[{}]新增员工：username={}, merchantId={}, role={}",
                    emp.getName(), dto.getUsername(), dto.getMerchantId(), dto.getRole());
        } else if (emp.getRole() != null && emp.getRole() == 1) {
            // 【权限】老板：只能新增到自己的商家，且 role 必须是 2（普通员工）
            dto.setMerchantId(emp.getMerchantId());
            if (dto.getRole() == null || dto.getRole() != 2) {
                dto.setRole(2);
            }
            log.info("老板[{}]新增员工到商家[{}]：username={}",
                    emp.getName(), emp.getMerchantId(), dto.getUsername());
        } else {
            throw new BusinessException("无权限新增员工");
        }

        Employee employee = new Employee();
        BeanUtils.copyProperties(dto, employee);
        employee.setStatus(StatusConstant.ENABLE);
        employee.setPassword(BCrypt.hashpw(PasswordConstant.DEFAULT_PASSWORD, BCrypt.gensalt()));
        save(employee);
    }

    @Override
    public Employee getById(Long id) {
        Employee emp = getEmp();
        Employee target = super.getById(id);
        if (target == null) {
            throw new BusinessException("员工不存在");
        }

        // 【权限】超管无限制；老板只能查看自己商家的员工
        if (emp.getRole() != null && emp.getRole() != 0) {
            if (emp.getMerchantId() == null || !emp.getMerchantId().equals(target.getMerchantId())) {
                throw new BusinessException("无权查看该员工");
            }
        }

        target.setPassword("******");
        return target;
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        Employee emp = getEmp();
        Employee target = super.getById(id);
        if (target == null) {
            throw new BusinessException("员工不存在");
        }

        // 【权限】超管无限制；老板只能操作自己商家的员工
        checkEmployeePermission(emp, target);
        log.info("员工[{}]状态变更：{}", target.getName(), status == 1 ? "启用" : "禁用");

        Employee build = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(build);
    }

    @Override
    public void update(EmployeeDTO dto) {
        Employee emp = getEmp();
        Employee target = super.getById(dto.getId());
        if (target == null) {
            throw new BusinessException("员工不存在");
        }

        // 【权限】超管可改任意；老板只能改自己商家的员工
        checkEmployeePermission(emp, target);

        if (emp.getRole() != null && emp.getRole() != 0) {
            // 【权限】老板不可修改 role 为 0 或 1（只能保持为 2）
            if (dto.getRole() != null && (dto.getRole() == 0 || dto.getRole() == 1)) {
                throw new BusinessException("无权设置超管或老板角色");
            }
            // 【权限】老板不可修改 merchantId
            dto.setMerchantId(emp.getMerchantId());
        }

        Employee employee = new Employee();
        BeanUtils.copyProperties(dto, employee);
        employeeMapper.update(employee);
        log.info("员工[{}]信息已更新", target.getName());
    }

    // ==================== 私有方法 ====================

    private Employee getEmp() {
        Employee emp = AuthContext.getCurrentEmployee();
        if (emp == null) {
            throw new BusinessException("未登录");
        }
        return emp;
    }

    /**
     * 解析商家ID：超管可按传入值查询（传null即全查），老板强制为自己的merchantId
     */
    private Long resolveMerchantId(Employee emp, Long requestMerchantId) {
        if (emp.getRole() != null && emp.getRole() == 0) {
            return requestMerchantId;
        }
        return emp.getMerchantId();
    }

    /**
     * 校验员工操作权限：超管无限制；老板只能操作自己商家的员工
     */
    private void checkEmployeePermission(Employee emp, Employee target) {
        if (emp.getRole() != null && emp.getRole() == 0) {
            return;
        }
        if (emp.getMerchantId() == null || !emp.getMerchantId().equals(target.getMerchantId())) {
            throw new BusinessException("无权操作该员工");
        }
    }
}
