package com.zyj.userservice.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * @Author：zyj
 * @Package：com.zyj.userservice.mapper
 * @Project：yun-shan
 * @name：UserMapper
 * @Date：09 12月 2025  18:18
 * @Filename：UserMapper
 */
@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByOpenId(String openId);


    void insert(User user);

    /**
     * 统计用户数量
     * @param map
     * @return
     */
    Integer countByMap(Map map);
}
