package com.bite.system.support;

import com.bite.common.core.enums.ResultCode;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 校验当前请求是否携带有效 Redis 登录态，且会话 {@code identity} 为管理端 {@code sysUser}。
 * <p>
 * 与 {@link com.bite.system.service.impl.SysUserServiceImpl#login} 写入 Redis 的 JSON 字段一致。
 */
@Component
public class SysUserOperatorVerifier {

    /** 与登录时写入 Redis 的 identity 值一致 */
    public static final String IDENTITY_SYS_USER = "sysUser";

    private final LoginSessionRedisService loginSessionRedisService;
    private final ObjectMapper objectMapper;

    @Value("${app.security.operator-id-header:X-User-Id}")
    private String operatorIdHeaderName;

    public SysUserOperatorVerifier(
            LoginSessionRedisService loginSessionRedisService,
            ObjectMapper objectMapper) {
        this.loginSessionRedisService = loginSessionRedisService;
        this.objectMapper = objectMapper;
    }

    /**
     * @return {@code null} 表示通过；否则为应原样返回前端的错误 {@link Result}
     */
    @Nullable
    public Result<Void> verifyOrFail() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return new Result<>("无法获取请求上下文", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            // token 头存在性统一由 gateway 校验；下游不再因为“未携带 token”直接拦截。
            return null;
        }
        String payload = loginSessionRedisService.getLoginPayload(token);
        if (!StringUtils.hasText(payload)) {
            return new Result<>("登录态已失效，请重新登录", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        String identity = extractIdentity(payload);
        if (!IDENTITY_SYS_USER.equals(identity)) {
            return new Result<>(
                    "仅管理端账号（SysUser）可执行题目相关操作",
                    ResultCode.FAILED_UNAUTHORIZED.getCode(),
                    null);
        }
        return null;
    }

    /**
     * 从当前请求头解析操作人 id（网关鉴权通过后会注入）；非法或缺失返回 null。
     */
    @Nullable
    public Long getOperatorUserIdFromHeader() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String raw = request.getHeader(operatorIdHeaderName);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 解析当前操作人 userId：优先 {@link #getOperatorUserIdFromHeader()}（网关注入），
     * 若缺失则从 Redis 登录态 JSON 的 {@code userId} 读取（直连 oj-system、或未转发自定义头时可用）。
     */
    @Nullable
    public Long resolveOperatorUserId() {
        Long fromHeader = getOperatorUserIdFromHeader();
        if (fromHeader != null) {
            return fromHeader;
        }
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String payload = loginSessionRedisService.getLoginPayload(token);
        return parseUserIdFromLoginPayload(payload);
    }

    @Nullable
    private Long parseUserIdFromLoginPayload(String loginInfoJson) {
        if (!StringUtils.hasText(loginInfoJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(loginInfoJson);
            JsonNode uid = root.get("userId");
            if (uid == null || uid.isNull()) {
                return null;
            }
            if (uid.isIntegralNumber()) {
                return uid.longValue();
            }
            String s = uid.asText();
            if (!StringUtils.hasText(s)) {
                return null;
            }
            return Long.parseLong(s.trim());
        } catch (JsonProcessingException | NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest();
    }

    private static String resolveToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    @Nullable
    private String extractIdentity(String loginInfoJson) {
        try {
            JsonNode root = objectMapper.readTree(loginInfoJson);
            JsonNode idNode = root.get("identity");
            if (idNode == null || idNode.isNull()) {
                return null;
            }
            return idNode.asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
