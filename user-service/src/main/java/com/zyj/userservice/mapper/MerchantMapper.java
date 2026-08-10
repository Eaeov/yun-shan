package com.zyj.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MerchantMapper extends BaseMapper<Merchant> {

    @Update("update merchant set status = #{status} where id = #{id}")
    int updateStatus(Long id, Integer status);
}
