package com.bite.friend.controller;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendUser;
import com.bite.friend.domain.dto.EmailCodeLoginRequest;
import com.bite.friend.domain.dto.EmailPasswordLoginRequest;
import com.bite.friend.domain.dto.PhonePasswordLoginRequest;
import com.bite.friend.domain.dto.SendEmailCodeRequest;
import com.bite.friend.domain.dto.UserAvatarObjectKeyRequest;
import com.bite.friend.domain.dto.UserAvatarStsRequest;
import com.bite.friend.domain.dto.UserLogoutRequest;
import com.bite.friend.domain.dto.UserRegisterRequest;
import com.bite.friend.service.FriendAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * OJFRIEND 用户认证与账户信息接口。
 * <p>
 * 设计原则：
 * 1) Controller 只负责参数接收和轻量校验；
 * 2) 业务处理（注册、登录、登出、验证码）统一下沉到 service；
 * 3) 接口统一返回 Result，方便前端按 code/msg/data 处理。
 */
@RestController
@RequestMapping("/friend/user")
public class UserOperationController {

    private final FriendAuthService friendAuthService;

    public UserOperationController(FriendAuthService friendAuthService) {
        this.friendAuthService = friendAuthService;
    }

    /** 用户注册。 */
    @PostMapping("/register")
    public Result<Long> register(@RequestBody UserRegisterRequest request) {
        return friendAuthService.register(request);
    }

    /** 手机号 + 密码登录。 */
    @PostMapping("/loginByPhonePassword")
    public Result<Map<String, Object>> loginByPhonePassword(@RequestBody PhonePasswordLoginRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        return friendAuthService.loginByPhonePassword(request.getPhone(), request.getPassword());
    }

    /** 邮箱 + 密码登录。 */
    @PostMapping("/loginByEmailPassword")
    public Result<Map<String, Object>> loginByEmailPassword(@RequestBody EmailPasswordLoginRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        return friendAuthService.loginByEmailPassword(request.getEmail(), request.getPassword());
    }

    /** 用户登出：按 userId 失效登录态并更新数据库状态。 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody UserLogoutRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        return friendAuthService.logout(request.getUserId());
    }

    /** 按 userId 查询用户详情（返回时不暴露密码）。 */
    @GetMapping("/detail")
    public Result<FriendUser> getUserDetail(@RequestParam("userId") Long userId) {
        return friendAuthService.getUserById(userId);
    }

    /**
     * 查询当前登录用户详情（无入参，必须携带 token）。
     * <p>
     * userId 从 token 登录态中解析；经网关时可用 X-User-Id 兜底。
     */
    @GetMapping("/me/detail")
    public Result<FriendUser> getCurrentUserDetail(
            HttpServletRequest request,
            @RequestParam(value = "token", required = false) String tokenParam,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader) {
        String token = extractToken(request, tokenParam);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        return friendAuthService.getCurrentUserDetail(token, gatewayUserId);
    }

    /**
     * 用户设置头像 object key（新增/修改统一接口）。
     * <p>
     * 入参仅 objectKey；userId 从 token 登录态中解析。
     */
    @PutMapping("/avatar/object-key")
    public Result<Map<String, String>> saveMyAvatarObjectKey(
            HttpServletRequest request,
            @RequestBody UserAvatarObjectKeyRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader) {
        if (body == null || !StringUtils.hasText(body.getObjectKey())) {
            return Result.fail("objectKey 不能为空");
        }
        String token = extractToken(request, null);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        Result<String> result = friendAuthService.saveMyAvatarObjectKey(token, gatewayUserId, body.getObjectKey());
        if (result.getCode() != 1000 || result.getData() == null) {
            return Result.fail(result.getMsg());
        }
        Map<String, String> data = new java.util.LinkedHashMap<>();
        data.put("objectKey", result.getData());
        return Result.ok("success", data);
    }

    /**
     * 申请头像上传 STS（前端直传 OSS）。
     */
    @PostMapping("/avatar/sts")
    public Result<Map<String, String>> issueMyAvatarSts(
            HttpServletRequest request,
            @RequestBody(required = false) UserAvatarStsRequest body,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader) {
        String token = extractToken(request, null);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        String dir = body == null ? null : body.getDir();
        return friendAuthService.issueMyAvatarSts(token, gatewayUserId, dir);
    }

    /**
     * 查看当前登录用户头像信息（object key + url）。
     */
    @GetMapping("/avatar")
    public Result<Map<String, String>> getMyAvatar(
            HttpServletRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String xUserIdHeader) {
        String token = extractToken(request, null);
        Long gatewayUserId = parseOptionalLong(xUserIdHeader);
        return friendAuthService.getMyAvatar(token, gatewayUserId);
    }

    /** 发送邮箱验证码。 */
    @PostMapping("/sendEmailCode")
    public Result<Void> sendEmailCode(@RequestBody SendEmailCodeRequest request) {
        return friendAuthService.sendEmailCode(request == null ? null : request.getEmail());
    }

    /** 邮箱 + 验证码登录。 */
    @PostMapping("/loginByEmailCode")
    public Result<String> loginByEmailCode(@RequestBody EmailCodeLoginRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        return friendAuthService.loginByEmailCode(request.getEmail(), request.getCode());
    }

    private static String extractToken(HttpServletRequest request, String tokenParam) {
        String t = request.getHeader("token");
        if (StringUtils.hasText(t)) {
            return t.trim();
        }
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam.trim();
        }
        String queryToken = request.getParameter("token");
        return StringUtils.hasText(queryToken) ? queryToken.trim() : null;
    }

    private static Long parseOptionalLong(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

