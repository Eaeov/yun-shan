package com.sky.exception.handler;

import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;

@RestControllerAdvice // 全局异常处理类
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常：把 message 原样返回，前端按 Result 结构处理
    @ExceptionHandler(BaseException.class)
    public Result<String> handleBaseException(BaseException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    // SQL 异常：不暴露细节给前端
    @ExceptionHandler(SQLException.class)
    public Result<String> handleSqlException(SQLException e) {
        log.error("数据库异常", e);
        return Result.error("系统繁忙，请稍后重试");
    }

    // 兜底：任何未预期异常，记完整堆栈，返回统一文案
    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请稍后重试");
    }
}
