package com.sky.exception;

/**
 * 通用业务异常
 */
public class BusinessException extends BaseException {

    public BusinessException() {
    }

    public BusinessException(String msg) {
        super(msg);
    }
}
