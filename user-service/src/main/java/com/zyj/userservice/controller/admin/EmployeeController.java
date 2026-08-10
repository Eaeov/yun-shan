package com.zyj.userservice.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import com.zyj.userservice.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端-员工管理（多商家权限）
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;

    /**
     * 员工登录
     */
    @PostMapping("/login")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);
        Employee employee = employeeService.login(employeeLoginDTO);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        claims.put(JwtClaimsConstant.EMP_MERCHANT_ID, employee.getMerchantId());
        claims.put(JwtClaimsConstant.EMP_ROLE, employee.getRole());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();
        return Result.success(employeeLoginVO);
    }
    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 分页查询员工（超管全查，老板仅查自己商家）
     * merchantId由后端根据权限自动注入
     */
    @GetMapping("/page")
    public Result<PageResult<Employee>> page(EmployeePageQueryDTO dto) {
        log.info("员工分页查询：{}", dto);
        PageResult<Employee> result = employeeService.page(dto);
        return Result.success(result);
    }

    /**
     * 根据ID查员工详情（超管无限制，老板仅查自己商家）
     */
    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        log.info("查询员工详情：id={}", id);
        Employee employee = employeeService.getById(id);
        return Result.success(employee);
    }

    /**
     * 新增员工
     * 超管可新增任意角色、任意商家；老板只能新增到自己的商家且role必须是2
     */
    @PostMapping
    public Result<String> save(@RequestBody EmployeeDTO dto) {
        log.info("新增员工：{}", dto);
        employeeService.addEmployees(dto);
        return Result.success();
    }

    /**
     * 修改员工
     * 超管可改任意；老板只能改自己商家员工，且不能设role=0或1
     */
    @PutMapping
    public Result<String> update(@RequestBody EmployeeDTO dto) {
        log.info("修改员工：{}", dto);
        employeeService.update(dto);
        return Result.success();
    }

    /**
     * 启用/禁用员工
     * 超管无限制；老板只能操作自己商家员工
     */
    @PutMapping("/status/{status}/{id}")
    public Result<String> startOrStop(@PathVariable Integer status, @PathVariable Long id) {
        log.info("员工状态变更：id={}, status={}", id, status);
        employeeService.startOrStop(status, id);
        return Result.success();
    }
}
