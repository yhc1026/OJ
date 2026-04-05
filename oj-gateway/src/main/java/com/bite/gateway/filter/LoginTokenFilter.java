package com.bite.gateway.filter;

import com.bite.common.core.enums.ResultCode;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 网关登录态过滤器：
 * 1) {@code security.whitelist.paths} 中的路径放行（默认含 login、题目只读接口、friend 邮箱验证码登录）；
 * 2) 其余 /sysUser/**、/user/**、/question/**、/exam/**、/friend/** 需携带 token；
 * 3) Redis 校验：{@code token→详情 JSON} 存在且非空；{@code userId→token} 与当前 token 一致（顶号后旧 token 失效）；
 * 4) 校验通过后对两键滑动续期，时长 {@code security.token.ttl-seconds}（默认 43200，即 12 小时）。
 */
@Component
public class LoginTokenFilter implements GlobalFilter, Ordered {

    private final LoginSessionRedisService loginSessionRedisService;
    private final ObjectMapper objectMapper;

    /** 与登录写入一致：两键 TTL（秒），默认 43200=12 小时 */
    @Value("${security.token.ttl-seconds:43200}")
    private long tokenTtlSeconds;

    /**
     * 逗号分隔的完整路径白名单，例如：/sysUser/login,/sysUser/insertUser
     * 也可在 Nacos 的 oj-gateway.yml 中覆盖。
     */
    @Value("${security.whitelist.paths:/sysUser/login,/question/page,/question/list,/question/detail,/question/brief,/question/brief-by-title,/question/detail-by-title,/question/list-by-title-like,/question/list-by-difficulty,/friend/user/register,/friend/user/loginByPhonePassword,/friend/user/loginByEmailPassword,/friend/user/logout,/friend/user/sendEmailCode,/friend/user/loginByEmailCode,/friend/exam/list/active,/friend/exam/list/finished,/friend/exam/detail,/friend/search/question/by-id-like,/friend/search/question/by-title-like,/friend/search/question/by-difficulty}")
    private String whitelistPathsConfig;

    /** 与 oj-system {@code app.security.operator-id-header} 一致，转发给下游标识当前操作人 */
    @Value("${app.security.operator-id-header:X-User-Id}")
    private String operatorIdHeaderName;

    private Set<String> whitelistPaths;

    public LoginTokenFilter(LoginSessionRedisService loginSessionRedisService, ObjectMapper objectMapper) {
        this.loginSessionRedisService = loginSessionRedisService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initWhitelist() {
        whitelistPaths = Arrays.stream(whitelistPathsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = normalizePath(exchange.getRequest().getURI().getPath());
        if (!needCheck(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "未登录，请先登录");
        }

        String loginInfo = loginSessionRedisService.getLoginPayload(token);
        if (!StringUtils.hasText(loginInfo)) {
            return unauthorized(exchange, "登录态已失效，请重新登录");
        }

        String operatorUserId = extractUserIdFromLoginJson(loginInfo);
        if (!loginSessionRedisService.matchesActiveToken(operatorUserId, token)) {
            return unauthorized(exchange, "账号已在其他终端登录，请使用最新令牌重新登录");
        }

        loginSessionRedisService.refreshSessionTtl(token, operatorUserId, tokenTtlSeconds);

        // 将登录态中的 userId 写入请求头，供下游 insertUser 等接口填充 createBy / updateBy
        ServerHttpRequest downstreamRequest = exchange.getRequest();
        if (StringUtils.hasText(operatorUserId)) {
            downstreamRequest = downstreamRequest.mutate()
                    .header(operatorIdHeaderName, operatorUserId)
                    .build();
        }
        return chain.filter(exchange.mutate().request(downstreamRequest).build());
    }

    private String extractUserIdFromLoginJson(String loginInfoJson) {
        try {
            JsonNode root = objectMapper.readTree(loginInfoJson);
            JsonNode uid = root.get("userId");
            if (uid == null || uid.isNull()) {
                return null;
            }
            return uid.asText();
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean needCheck(String path) {
        if (isBuiltInPublicPath(path)) {
            return false;
        }
        if (path.startsWith("/sysUser")
                || path.startsWith("/user")
                || path.startsWith("/question")
                || path.startsWith("/exam")
                || path.startsWith("/friend")) {
            return !whitelistPaths.contains(path);
        }
        return false;
    }

    /**
     * 代码内置白名单兜底：避免 Nacos 白名单未及时生效导致登录前接口被误拦截。
     */
    private boolean isBuiltInPublicPath(String path) {
        return "/sysUser/login".equals(path)
                || "/question/page".equals(path)
                || "/question/list".equals(path)
                || "/question/detail".equals(path)
                || "/question/brief".equals(path)
                || "/question/brief-by-title".equals(path)
                || "/question/detail-by-title".equals(path)
                || "/question/list-by-title-like".equals(path)
                || "/question/list-by-difficulty".equals(path)
                || "/friend/user/register".equals(path)
                || "/friend/user/loginByPhonePassword".equals(path)
                || "/friend/user/loginByEmailPassword".equals(path)
                || "/friend/user/logout".equals(path)
                || "/friend/user/sendEmailCode".equals(path)
                || "/friend/user/loginByEmailCode".equals(path)
                || "/friend/exam/list/active".equals(path)
                || "/friend/exam/list/finished".equals(path)
                || "/friend/exam/detail".equals(path)
                || "/friend/search/question/by-id-like".equals(path)
                || "/friend/search/question/by-title-like".equals(path)
                || "/friend/search/question/by-difficulty".equals(path);
    }

    /** 去掉末尾 /，避免 /sysUser/login/ 与白名单不匹配 */
    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String extractToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst("token");
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        String queryToken = request.getQueryParams().getFirst("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken.trim();
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = toJsonBytes(new Result<>(message, ResultCode.FAILED_UNAUTHORIZED.getCode(), null));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    private byte[] toJsonBytes(Result<?> result) {
        try {
            return objectMapper.writeValueAsBytes(result);
        } catch (JsonProcessingException e) {
            return "{\"msg\":\"未授权\",\"code\":3001}".getBytes(StandardCharsets.UTF_8);
        }
    }
}
