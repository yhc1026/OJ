package com.bite.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.bite.system.common.config.SysUserTokenHeaderFilter;
import com.bite.system.domain.SysUser;
import com.bite.system.mapper.SysUserMapper;
import com.bite.system.service.SysUserService;
import com.bite.utils.JwtTokenUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端用户（SysUser）业务服务实现。
 * <p>
 * 默认实现已包含常用 CRUD（save/getById/list/updateById/removeById 等）。
 * <p>
 * 与 {@link com.bite.system.controller.SysUserController} 对应的对外接口中，除 {@code /sysUser/login} 外，
 * 均由 {@link SysUserTokenHeaderFilter} 强制要求请求携带 {@code token}（或 Bearer），
 * 保证经网关访问时每次操作都会参与 Redis 登录态校验与续期，避免长时间操作中途被判定登出。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * MD5 加盐用的盐值（可通过环境变量 PWD_SALT 覆盖）。
     */
    @Value("${security.password.salt:oj-salt}")
    private String passwordSalt;

    @Value("${security.jwt.secret:oj-jwt-secret-key-please-change-2026}")
    private String jwtSecret;

    @Value("${security.jwt.expire-seconds:604800}")
    private long jwtExpireSeconds;

    /** 与网关一致：Redis 两键（token→详情、userId→token）过期时间（秒），默认 43200=12 小时 */
    @Value("${security.token.ttl-seconds:43200}")
    private long tokenTtlSeconds;

    /**
     * 当前操作人用户 id 所在请求头（网关鉴权通过后会从 Redis 注入，也可由前端在可信链路下传递）。
     */
    @Value("${app.security.operator-id-header:X-User-Id}")
    private String operatorIdHeaderName;

    private final LoginSessionRedisService loginSessionRedisService;
    private final ObjectMapper objectMapper;

    public SysUserServiceImpl(LoginSessionRedisService loginSessionRedisService, ObjectMapper objectMapper) {
        this.loginSessionRedisService = loginSessionRedisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Map<String, Object>> login(String userAccount, String pwd) {
        if (!StringUtils.hasText(userAccount)) {
            return Result.fail("userAccount 不能为空");
        }
        if (!StringUtils.hasText(pwd)) {
            return Result.fail("pwd 不能为空");
        }

        SysUser user = this.getOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserAccount, userAccount)
        );
        if (user == null) {
            return Result.fail("用户不存在");
        }

        String stored = user.getPassword();
        if (!StringUtils.hasText(stored)) {
            return Result.fail("请填写密码");
        }

        String incomingEncrypted = md5WithSalt(pwd);
        boolean ok = stored.equalsIgnoreCase(incomingEncrypted);

        if (!ok) {
            return Result.fail("用户名或密码错误");
        }

        // 1) JWT 中不放用户业务信息，只用于登录态标识
        String token = JwtTokenUtils.generateToken(jwtSecret, jwtExpireSeconds);

        // 2) 登录会话写入 Redis（oj-common-redis 封装：顶号时删旧 LoginSessionPayloadByToken-*）
        Long uid = user.getUserId();
        Map<String, Object> tokenValue = new LinkedHashMap<>();
        tokenValue.put("token", token);
        tokenValue.put("userId", user.getUserId());
        tokenValue.put("nickName", user.getNickName());
        tokenValue.put("identity", "sysUser");
        try {
            String redisValue = objectMapper.writeValueAsString(tokenValue);
            loginSessionRedisService.saveOrReplaceSession(
                    uid,
                    token,
                    redisValue,
                    Duration.ofSeconds(tokenTtlSeconds)
            );
        } catch (JsonProcessingException e) {
            return Result.fail("登录态缓存写入失败");
        }

        // 返回给前端：token + 基础展示信息
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        // Return as string to avoid JS precision loss on large Long.
        response.put("userId", String.valueOf(user.getUserId()));
        response.put("nickName", user.getNickName());
        response.put("identity", "sysUser");
        return Result.ok("校验成功", response);
    }

    @Override
    public Result<Void> logout() {
        String token = resolveTokenFromRequest();
        if (!StringUtils.hasText(token)) {
            return new Result<>("缺少 token", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }

        Long headerUid = resolveOperatorIdFromRequestHeader();
        String userIdFromHeader = headerUid != null ? String.valueOf(headerUid) : null;

        String payload = loginSessionRedisService.getLoginPayload(token);
        String userIdFromPayload = extractUserIdFromLoginJson(payload);

        if (StringUtils.hasText(userIdFromHeader) && StringUtils.hasText(userIdFromPayload)
                && !userIdFromHeader.equals(userIdFromPayload)) {
            return new Result<>("登录身份不一致", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }

        String userId = StringUtils.hasText(userIdFromHeader) ? userIdFromHeader : userIdFromPayload;
        if (!StringUtils.hasText(userId)) {
            return new Result<>(
                    "无法确定当前用户，请在请求头携带 " + operatorIdHeaderName + " 或确保 token 对应有效会话",
                    ResultCode.FAILED_UNAUTHORIZED.getCode(),
                    null);
        }

        // 主流程：按 userId 在 Redis 找到活动 JWT，删除 LoginSessionPayloadByToken 与 ActiveLoginTokenByUserId
        loginSessionRedisService.invalidateByUserId(userId);
        // 防止顶号/并发下当前请求 JWT 仍残留单独会话 key
        loginSessionRedisService.removeSession(token, null);
        return Result.ok(ResultCode.SUCCESS.getMsg(), null);
    }

    @Override
    public Result<Boolean> insertUser(SysUser body) {
        if (body == null) {
            return new Result<>("请求体不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        Long operatorId = resolveOperatorIdFromRequestHeader();
        if (operatorId == null) {
            return new Result<>(
                    "缺少操作人信息，请在请求头携带 " + operatorIdHeaderName + "（当前登录用户 id）",
                    ResultCode.FAILED_PARAMS_VALIDATE.getCode(),
                    null);
        }

        // 密码：MD5+盐（与登录校验一致）
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            body.setPassword(md5WithSalt(body.getPassword()));
        }

        // 审计字段：createBy / updateBy 均以实际修改者为准（必填，来自 Header）
        body.setCreateBy(operatorId);
        body.setUpdateBy(operatorId);
        LocalDateTime now = LocalDateTime.now();
        if (body.getCreateTime() == null) {
            body.setCreateTime(now);
        }
        body.setUpdateTime(now);

        boolean ok = save(body);
        if (!ok) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    private String resolveTokenFromRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        String t = request.getHeader("token");
        if (StringUtils.hasText(t)) {
            return t.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        return null;
    }

    private String extractUserIdFromLoginJson(String loginInfoJson) {
        if (!StringUtils.hasText(loginInfoJson)) {
            return null;
        }
        try {
            var root = objectMapper.readTree(loginInfoJson);
            var uid = root.get("userId");
            if (uid == null || uid.isNull()) {
                return null;
            }
            return uid.asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * 从当前 HTTP 请求头解析操作人 id；非法或缺失返回 null。
     */
    private Long resolveOperatorIdFromRequestHeader() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
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

    private String md5WithSalt(String rawPwd) {
        // 约定：MD5( rawPwd + salt )
        String text = rawPwd + (passwordSalt == null ? "" : passwordSalt);
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}

