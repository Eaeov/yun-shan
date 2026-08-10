package com.sky.exception;

/**
 * 无权限异常
 */
public class OrderPermissionDeniedException extends BaseException {

    public OrderPermissionDeniedException(String msg) {
        super(msg);
    }
}
