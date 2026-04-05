package com.bite.common.redis.session;

import com.bite.common.core.redis.LoginRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 登录会话在 Redis 中的读写封装。
 * <p>
 * 两键一致过期：{@code LoginSessionPayloadByToken-{jwt}} → 用户详情 JSON；
 * {@code ActiveLoginTokenByUserId-{userId}} → 当前 JWT。
 * 网关在每次带 token 校验通过后调用 {@link #refreshSessionTtl}，将两键 TTL 重置为与登录相同的秒数（默认 43200=12h，滑动续期）。
 * <p>
 * 兼容旧键名 {@code login_tokens:} / {@code login_user_active:}（读、续期、顶号删除），新登录只写新键。
 */
@Service
public class LoginSessionRedisService {

    private final RedisOperatorService redisOperatorService;

    public LoginSessionRedisService(RedisOperatorService redisOperatorService) {
        this.redisOperatorService = redisOperatorService;
    }

    public void saveOrReplaceSession(Long userId, String token, String loginJsonPayload, Duration ttl) {
        if (userId != null) {
            String uid = String.valueOf(userId);
            String activeKey = LoginRedisKeys.userActiveKey(uid);
            String oldToken = firstNonBlank(redisOperatorService.get(activeKey), redisOperatorService.get(LoginRedisKeys.legacyUserActiveKey(uid)));
            if (StringUtils.hasText(oldToken)) {
                redisOperatorService.delete(LoginRedisKeys.loginTokenKey(oldToken));
                redisOperatorService.delete(LoginRedisKeys.legacyLoginTokenKey(oldToken));
            }
        }
        redisOperatorService.set(LoginRedisKeys.loginTokenKey(token), loginJsonPayload, ttl);
        if (userId != null) {
            String uid = String.valueOf(userId);
            redisOperatorService.set(LoginRedisKeys.userActiveKey(uid), token, ttl);
            redisOperatorService.delete(LoginRedisKeys.legacyUserActiveKey(uid));
        }
    }

    public String getLoginPayload(String token) {
        String v = redisOperatorService.get(LoginRedisKeys.loginTokenKey(token));
        if (StringUtils.hasText(v)) {
            return v;
        }
        return redisOperatorService.get(LoginRedisKeys.legacyLoginTokenKey(token));
    }

    /** 网关鉴权：新键或兼容旧键任一侧有有效 JSON 即视为已登录。 */
    public boolean hasLoginSession(String token) {
        return StringUtils.hasText(getLoginPayload(token));
    }

    public boolean matchesActiveToken(String userIdStr, String token) {
        if (!StringUtils.hasText(userIdStr)) {
            return true;
        }
        String active = firstNonBlank(
                redisOperatorService.get(LoginRedisKeys.userActiveKey(userIdStr)),
                redisOperatorService.get(LoginRedisKeys.legacyUserActiveKey(userIdStr))
        );
        if (!StringUtils.hasText(active)) {
            return true;
        }
        return active.equals(token);
    }

    public Long getLoginTokenTtlSeconds(String token) {
        Long t = redisOperatorService.getExpire(LoginRedisKeys.loginTokenKey(token), TimeUnit.SECONDS);
        if (t != null && t != -2L) {
            return t;
        }
        return redisOperatorService.getExpire(LoginRedisKeys.legacyLoginTokenKey(token), TimeUnit.SECONDS);
    }

    public void refreshSessionTtl(String token, String userIdStr, long ttlSeconds) {
        redisOperatorService.expire(LoginRedisKeys.loginTokenKey(token), ttlSeconds, TimeUnit.SECONDS);
        redisOperatorService.expire(LoginRedisKeys.legacyLoginTokenKey(token), ttlSeconds, TimeUnit.SECONDS);
        if (StringUtils.hasText(userIdStr)) {
            redisOperatorService.expire(LoginRedisKeys.userActiveKey(userIdStr), ttlSeconds, TimeUnit.SECONDS);
            redisOperatorService.expire(LoginRedisKeys.legacyUserActiveKey(userIdStr), ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public void removeSession(String token, Long userId) {
        redisOperatorService.delete(LoginRedisKeys.loginTokenKey(token));
        redisOperatorService.delete(LoginRedisKeys.legacyLoginTokenKey(token));
        if (userId != null) {
            String uid = String.valueOf(userId);
            redisOperatorService.delete(LoginRedisKeys.userActiveKey(uid));
            redisOperatorService.delete(LoginRedisKeys.legacyUserActiveKey(uid));
        }
    }

    public void invalidateByUserId(String userIdStr) {
        if (!StringUtils.hasText(userIdStr)) {
            return;
        }
        String id = userIdStr.trim();
        String activeKey = LoginRedisKeys.userActiveKey(id);
        String jwt = redisOperatorService.get(activeKey);
        if (!StringUtils.hasText(jwt)) {
            jwt = redisOperatorService.get(LoginRedisKeys.legacyUserActiveKey(id));
        }
        if (StringUtils.hasText(jwt)) {
            redisOperatorService.delete(LoginRedisKeys.loginTokenKey(jwt));
            redisOperatorService.delete(LoginRedisKeys.legacyLoginTokenKey(jwt));
        }
        redisOperatorService.delete(activeKey);
        redisOperatorService.delete(LoginRedisKeys.legacyUserActiveKey(id));
    }

    private static String firstNonBlank(String a, String b) {
        if (StringUtils.hasText(a)) {
            return a;
        }
        return StringUtils.hasText(b) ? b : null;
    }
}
