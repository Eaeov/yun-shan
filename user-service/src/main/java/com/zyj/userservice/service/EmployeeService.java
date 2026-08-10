package com.zyj.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService extends IService<Employee> {

    PageResult<Employee> page(EmployeePageQueryDTO dto);

    Employee login(EmployeeLoginDTO dto);

    void addEmployees(EmployeeDTO dto);

    Employee getById(Long id);

    void startOrStop(Integer status, Long id);

    void update(EmployeeDTO dto);
}
