package com.example.mallu.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或 token 已失效"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    USER_EXISTS(1001, "用户已存在"),
    USER_NOT_FOUND(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    TOKEN_INVALID(1004, "token 无效或已过期"),

    ADDRESS_NOT_FOUND(2001, "收货地址不存在"),
    ADDRESS_LIMIT_EXCEEDED(2002, "收货地址数量超过上限"),

    CART_EMPTY(3001, "购物车为空"),
    CART_ITEM_NOT_FOUND(3002, "购物车记录不存在"),

    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态不正确"),
    STOCK_NOT_ENOUGH(4003, "商品库存不足"),

    PAY_TRADE_EXISTS(4101, "支付流水号重复"),
    PAY_AMOUNT_NOT_MATCH(4102, "回调金额与订单金额不一致"),

    COUPON_NOT_FOUND(5001, "优惠券不存在"),
    COUPON_NOT_AVAILABLE(5002, "优惠券不可用"),
    USER_COUPON_NOT_FOUND(5003, "用户优惠券不存在"),
    USER_COUPON_NOT_AVAILABLE(5004, "用户优惠券不可用或已使用"),

    RATE_LIMIT(6001, "请求过于频繁，请稍后重试"),
    REDIS_UNAVAILABLE(8001, "关键服务暂不可用，请稍后重试"),

    SECKILL_ACTIVITY_NOT_FOUND(7001, "秒杀活动不存在或已结束"),
    SECKILL_GOODS_NOT_FOUND(7002, "秒杀商品不存在"),
    SECKILL_ALREADY_PURCHASED(7003, "您已参与过本次秒杀"),
    SECKILL_STOCK_EMPTY(7004, "秒杀商品已售罄"),
    SECKILL_ORDER_NOT_FOUND(7005, "秒杀订单不存在");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
