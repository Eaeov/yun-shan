package com.sky.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号被锁定";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String CATEGORY_BE_RELATED_BY_SETMEAL = "当前分类关联了套餐,不能删除";
    public static final String CATEGORY_BE_RELATED_BY_DISH = "当前分类关联了菜品,不能删除";
    public static final String SHOPPING_CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String ADDRESS_BOOK_IS_NULL = "用户地址为空，不能下单";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String UPLOAD_FAILED = "文件上传失败";
    public static final String SETMEAL_ENABLE_FAILED = "套餐内包含未启售菜品，无法启售";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";
    public static final String DISH_ON_SALE = "起售中的菜品不能删除";
    public static final String SETMEAL_ON_SALE = "起售中的套餐不能删除";
    public static final String DISH_BE_RELATED_BY_SETMEAL = "当前菜品关联了套餐,不能删除";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String STATE_NOT_FOUNT = "不存在的修改状态";
    public static final String USER_INFO = "user_info"; // 网关传递的用户信息
    public static final String GATEWAY_SECRET_KEY = "gatewaySecretKey";
    public static final String INTERNAL_CALL_HEADER = "X-Internal-Call"; // 内部服务间调用标识
    public static final String INTERNAL_CALL_VALUE = "true";
    public static final String MERCHANT_ID_CAN_NOT_BE_EMPTY = "商家ID不能为空";
    public static final String ORDER_PERMISSION_DENIED = "无权操作该订单";
    public static final String MERCHANT_SHOP_CLOSED = "商家已打烊，无法下单";
    public static final String MERCHANT_PERMISSION_DENIED = "无权限操作该商家资源";
    public static final String CATEGORY_NAME_EXISTS = "该商家下已存在同名分类";
    public static final String REVIEW_ALREADY_EXISTS = "您已评论过该订单的该菜品";
    public static final String REVIEW_PERMISSION_DENIED = "无权操作该评论";
    public static final String ORDER_NOT_COMPLETED = "订单未完成，无法评论";
    public static final String NO_COMMODITY_EXISTS = "不存在该商品";
    public static final String ORDER_STATUS_UNDELIVERABLE = "当前订单状态不可催单";
}