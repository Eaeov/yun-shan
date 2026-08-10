package com.zyj.productservice.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @Author：zyj
 * @Package：com.zyj.productservice.controller.admin
 * @Project：yun-shan
 * @name：CommonController
 * @Date：06 12月 2025  15:09
 * @Filename：CommonController
 * 通用接口
 */
@RequestMapping("/admin/common")
@RestController
@Slf4j
@RequiredArgsConstructor
public class CommonController {

    private final AliOssUtil aliOssUtil;


    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file){
        log.info("文件上传：{}",file);

        try {
            // 原始文件名
            String originalFilename = file.getOriginalFilename();
            // 截取文件名的后缀
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 构建新的文件名称
            String objetName = UUID.randomUUID().toString() + extension;
            // filePath 文件的请求路径
            String filePath = aliOssUtil.upload(file.getBytes(), objetName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败：{}",e);
        }
        //UPLOAD_FAILED=文件上传失败
        return Result.error(MessageConstant.UPLOAD_FAILED);
    }
}
