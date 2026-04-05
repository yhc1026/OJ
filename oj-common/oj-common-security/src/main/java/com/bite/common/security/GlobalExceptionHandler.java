package com.bite.common.security;

import com.bite.common.core.enums.ResultCode;
import com.bite.domain.Result;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * <p>
 * 统一把异常转换为 {@link Result} 返回，方便前端统一处理。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Log log = LogFactory.getLog(GlobalExceptionHandler.class);

    /**
     * 请求方法不支持。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error("请求方法不支持: " + ex.getMessage(), ex);
        return new Result<>("请求方法不支持", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
    }

    /**
     * 拦截运行时异常（RuntimeException）。
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException ex) {
        log.error("拦截运行时异常: " + ex.getMessage(), ex);
        return new Result<>("拦截运行时异常", ResultCode.ERROR.getCode(), null);
    }

    /**
     * 系统异常兜底（Exception）。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常: " + ex.getMessage(), ex);
        return new Result<>("系统异常", ResultCode.ERROR.getCode(), null);
    }
}

