package com.zyj.userservice.service.impl;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.properties.WeChatProperties;
import com.sky.utils.HttpClientUtil;
import com.zyj.userservice.mapper.UserMapper;
import com.zyj.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * @Author：zyj
 * @Package：com.zyj.userservice.service.impl
 * @Project：yun-shan
 * @name：UserServiceImpl
 * @Date：09 12月 2025  17:21
 * @Filename：UserServiceImpl
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final WeChatProperties weChatProperties;
    private final UserMapper userMapper;

    public static final  String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    /**
     * 微信登入
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前用户的openId
        String openid = getOpenid(userLoginDTO.getCode());
        //判断openId是否为空，如果为空表示登入失败，抛出业务异常
        if(openid == null ){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断是否为新用户
        User user = userMapper.getByOpenId(openid);
        //如果是新用户，自动完成注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回用户对象

        return user;
    }

    /**
     * 调用微信接口，获得openid
     * @param code  登入凭证
     * @return
     */
    private String getOpenid(String code){
        HashMap<String, String> map = new HashMap<>();
        map.put("appid",weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type","authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");

        return openid;
    }
}
