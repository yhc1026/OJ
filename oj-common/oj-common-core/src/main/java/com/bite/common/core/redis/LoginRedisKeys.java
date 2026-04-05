package com.bite.common.core.redis;

/**
 * 登录态相关 Redis key 约定（oj-system、oj-friend 与 oj-gateway 共用）。
 * <p>
 * 命名：大驼峰语义段 + {@code -} + 动态段（如 userId、完整 JWT）。
 */
public final class LoginRedisKeys {

    private LoginRedisKeys() {
    }

    /**
     * 会话详情 String：{@code LoginSessionPayloadByToken-{完整 JWT}} → JSON（含 userId、identity 等）。
     */
    public static final String LOGIN_SESSION_PAYLOAD_PREFIX = "LoginSessionPayloadByToken-";

    /**
     * 用户当前有效 JWT String：{@code ActiveLoginTokenByUserId-{userId}} → 完整 JWT。
     * 同一 userId 再次登录时删除旧 {@link #LOGIN_SESSION_PAYLOAD_PREFIX} 条目，实现顶号 / 单会话。
     */
    public static final String ACTIVE_LOGIN_TOKEN_BY_USER_PREFIX = "ActiveLoginTokenByUserId-";

    public static String loginTokenKey(String token) {
        return LOGIN_SESSION_PAYLOAD_PREFIX + token;
    }

    public static String userActiveKey(String userId) {
        return ACTIVE_LOGIN_TOKEN_BY_USER_PREFIX + userId;
    }

    // ---------- 兼容重命名前已写入 Redis 的旧键（login_tokens: / login_user_active:）----------

    public static final String LEGACY_LOGIN_TOKEN_PREFIX = "login_tokens:";

    public static final String LEGACY_USER_ACTIVE_PREFIX = "login_user_active:";

    public static String legacyLoginTokenKey(String token) {
        return LEGACY_LOGIN_TOKEN_PREFIX + token;
    }

    public static String legacyUserActiveKey(String userId) {
        return LEGACY_USER_ACTIVE_PREFIX + userId;
    }
}
