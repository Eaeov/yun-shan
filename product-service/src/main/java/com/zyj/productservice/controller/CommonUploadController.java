package com.zyj.productservice.controller;

import com.sky.constant.MessageConstant;
import com.sky.exception.BusinessException;
import com.sky.result.Result;
import com.sky.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 通用上传接口（需 JWT 认证，由网关校验后携带 user_info 头）
 *
 * @Author：zyj
 * @Date：29 4月 2026
 */
@Tag(name = "通用接口")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/common/upload")
public class CommonUploadController {

    private final AliOssUtil aliOssUtil;
    private final StringRedisTemplate stringRedisTemplate;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final String RATE_LIMIT_KEY_PREFIX = "upload:rate:";

    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file,
                                       @RequestHeader(MessageConstant.USER_INFO) String userId) {
        log.info("上传图片，用户ID：{}，文件名：{}，大小：{}", userId, file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();

        // 1. 后缀白名单校验
        String extension = extractExtension(originalFilename);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new BusinessException("不支持的文件类型，仅允许 jpg、png、webp 格式");
        }

        // 2. MIME 类型校验（防伪造后缀）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("文件 MIME 类型不合法");
        }

        // 3. 文件大小限制（≤2MB）
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 2MB");
        }

        // 4. 单用户频率限制（1次/秒）
        String rateLimitKey = RATE_LIMIT_KEY_PREFIX + userId;
        Boolean allowed = stringRedisTemplate.opsForValue()
                .setIfAbsent(rateLimitKey, "1", 1, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(allowed)) {
            throw new BusinessException("上传过于频繁，请稍后再试");
        }

        try {
            String objectName = "review/" + UUID.randomUUID().toString().replace("-", "") + extension;
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            log.info("图片上传成功，用户ID：{}，URL: {}", userId, url);
            return Result.success(url);
        } catch (IOException e) {
            log.error("图片上传失败：{}", e.getMessage(), e);
            throw new BusinessException(MessageConstant.UPLOAD_FAILED);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}