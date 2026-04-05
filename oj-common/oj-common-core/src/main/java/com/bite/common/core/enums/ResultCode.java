package com.bite.common.core.enums;

/**
 * 统一返回码枚举。
 * <p>
 * code 用于机器识别；msg 用于人类友好提示（可被 Result 的自定义 msg 覆盖）。
 */
public enum ResultCode {
    SUCCESS(1000, "操作成功"),
    // 服务器内部错误，友好提示
    ERROR(2000, "服务繁忙请稍后重试"),
    // 操作失败，但是服务器不存在异常
    FAILED(3000, "操作失败"),
    FAILED_UNAUTHORIZED(3001, "未授权"),
    FAILED_PARAMS_VALIDATE(3002, "参数校验失败"),
    FAILED_NOT_EXISTS(3003, "资源不存在"),
    FAILED_ALREADY_EXISTS(3004, "资源已存在"),

    // 用户相关
    FAILED_USER_EXISTS(3101, "用户已存在"),
    FAILED_USER_NOT_EXISTS(3102, "用户不存在"),
    FAILED_LOGIN(3103, "用户名或密码错误"),
    FAILED_USER_BANNED(3104, "您已被列入黑名单, 请联系管理员.");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}

