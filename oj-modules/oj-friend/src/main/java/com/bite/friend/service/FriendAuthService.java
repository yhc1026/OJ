package com.bite.friend.service;

import com.bite.domain.Result;
import com.bite.friend.domain.FriendUser;
import com.bite.friend.domain.dto.UserRegisterRequest;

import java.util.Map;

/**
 * 用户认证与会话服务接口。
 */
public interface FriendAuthService {
    /** 用户注册，返回新用户 id。 */
    Result<Long> register(UserRegisterRequest request);

    /** 手机号密码登录，返回 token 与基础用户信息。 */
    Result<Map<String, Object>> loginByPhonePassword(String phone, String password);

    /** 邮箱密码登录，返回 token 与基础用户信息。 */
    Result<Map<String, Object>> loginByEmailPassword(String email, String password);

    /** 按用户 id 查询详情（隐藏密码）。 */
    Result<FriendUser> getUserById(Long userId);

    /**
     * 查询当前登录用户详情（无入参，依赖 token 解析 userId）。
     *
     * @param token         请求 token
     * @param gatewayUserId 网关注入的 X-User-Id（兜底）
     */
    Result<FriendUser> getCurrentUserDetail(String token, Long gatewayUserId);

    /**
     * 设置当前登录用户头像 object key（新增/修改统一接口）。
     *
     * @param token         请求 token
     * @param gatewayUserId 网关注入的 X-User-Id（兜底）
     * @param objectKey     OSS object key
     */
    Result<String> saveMyAvatarObjectKey(String token, Long gatewayUserId, String objectKey);

    /**
     * 为当前登录用户签发头像上传 STS（限制上传目录前缀）。
     *
     * @param token         请求 token
     * @param gatewayUserId 网关注入的 X-User-Id（兜底）
     * @param dir           可选目录片段
     */
    Result<Map<String, String>> issueMyAvatarSts(String token, Long gatewayUserId, String dir);

    /**
     * 查询当前登录用户头像信息（object key + 可访问 URL）。
     *
     * @param token         请求 token
     * @param gatewayUserId 网关注入的 X-User-Id（兜底）
     */
    Result<java.util.Map<String, String>> getMyAvatar(String token, Long gatewayUserId);

    /** 用户登出：清理登录态并更新状态。 */
    Result<Void> logout(Long userId);

    /** 发送邮箱验证码。 */
    Result<Void> sendEmailCode(String email);

    /** 邮箱验证码登录，返回 token。 */
    Result<String> loginByEmailCode(String email, String code);
}

