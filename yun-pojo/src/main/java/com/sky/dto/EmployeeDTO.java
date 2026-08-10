package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

    //商家ID（可为null，区分平台员工与商家员工）
    private Long merchantId;

    //角色 0=超级管理员 1=商家老板 2=普通员工
    private Integer role;
}
