package com.zyj.userservice.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;

/**
 * @Author：zyj
 * @Package：com.zyj.userservice.service
 * @Project：yun-shan
 * @name：UserService
 * @Date：09 12月 2025  17:20
 * @Filename：UserService
 */
public interface UserService {

    /**
     * 微信登入功能
     * @param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
