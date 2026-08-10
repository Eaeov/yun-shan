package com.zyj.yunapi.client;

import com.sky.entity.AddressBook;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.vo.MerchantVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * @Author：zyj
 * @Package：com.zyj.yunapi.client
 * @Project：yun-shan
 * @name：UserClient
 * @Date：10 3月 2026  16:33
 * @Filename：UserClient
 */
@FeignClient("user-service")
public interface UserClient {

    /**
     * 根据id查询地址
     * @param id
     * @return
     * 来自 addressBook
     */
    @GetMapping("/user/addressBook/{id}")
    Result<AddressBook> getAddressBookById(@PathVariable Long id);

    /**
     * 根据ID查商家详情（超管/老板自己）
     */
    @GetMapping("/{id}")
    Result<MerchantVO> getMerchantVOById(@PathVariable Long id);
}
