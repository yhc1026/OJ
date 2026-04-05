package com.bite.domain;

import com.bite.common.core.enums.ResultCode;

/**
 * 通用接口返回体。
 * <p>
 * code/msg 默认来源于 {@link ResultCode}，也支持业务侧传入自定义 msg。
 */
public class Result<T> {
    private String msg;
    private int code;
    private T data;

    public Result() {
    }

    public Result(String msg, int code) {
        this.msg = msg;
        this.code = code;
    }

    public Result(String msg, int code, T data) {
        this.msg = msg;
        this.code = code;
        this.data = data;
    }

    public static <T> Result<T> ok(String msg) {
        return new Result<>(msg, ResultCode.SUCCESS.getCode(), null);
    }

    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(msg, ResultCode.SUCCESS.getCode(), data);
    }

    public static <T> Result<T> ok(ResultCode code, T data) {
        return new Result<>(code.getMsg(), code.getCode(), data);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(msg, ResultCode.FAILED.getCode(), null);
    }

    public static <T> Result<T> fail(String msg, T data) {
        return new Result<>(msg, ResultCode.FAILED.getCode(), data);
    }

    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getMsg(), code.getCode(), null);
    }

    public static <T> Result<T> fail(ResultCode code, T data) {
        return new Result<>(code.getMsg(), code.getCode(), data);
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}

