package com.bite.friend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.bite.common.core.redis.FriendRedisKeys;
import com.bite.common.core.redis.LoginRedisKeys;
import com.bite.common.redis.core.RedisOperatorService;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.bite.common.file.config.OssProperties;
import com.bite.common.file.service.FileStorageService;
import com.bite.common.file.service.OssStsService;
import com.bite.common.file.service.impl.AliyunOssStsServiceImpl;
import com.bite.common.file.service.impl.DisabledOssStsServiceImpl;
import com.bite.friend.domain.FriendUser;
import com.bite.friend.domain.dto.UserRegisterRequest;
import com.bite.friend.mapper.FriendUserMapper;
import com.bite.friend.service.FriendAuthService;
import com.bite.utils.JwtTokenUtils;
import com.bite.utils.VerificationCodeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户认证业务实现。
 * <p>
 * 职责：
 * 1) 注册与密码登录（手机号/邮箱）；
 * 2) 邮箱验证码发送与验证码登录；
 * 3) 登录态缓存（active/token）维护与顶号处理；
 * 4) 登出时清理缓存并同步数据库状态。
 */
@Service
public class FriendAuthServiceImpl implements FriendAuthService {

    private static final long CODE_TTL_SECONDS = 59;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");

    private final FriendUserMapper friendUserMapper;
    private final RedisOperatorService redisOperatorService;
    private final JavaMailSender javaMailSender;
    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;
    private final OssStsService ossStsService;
    private final OssProperties ossProperties;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expire-seconds:604800}")
    private long jwtExpireSeconds;

    /** 与网关一致：token 详情 + userId→token 两键过期时间（默认 12 小时） */
    @Value("${security.token.ttl-seconds:43200}")
    private long loginSessionTtlSeconds;

    @Value("${security.password.salt:oj-salt}")
    private String passwordSalt;

    private final LoginSessionRedisService loginSessionRedisService;

    public FriendAuthServiceImpl(FriendUserMapper friendUserMapper,
                                 RedisOperatorService redisOperatorService,
                                 LoginSessionRedisService loginSessionRedisService,
                                 FileStorageService fileStorageService,
                                 OssStsService ossStsService,
                                 OssProperties ossProperties,
                                 JavaMailSender javaMailSender,
                                 ObjectMapper objectMapper) {
        this.friendUserMapper = friendUserMapper;
        this.redisOperatorService = redisOperatorService;
        this.loginSessionRedisService = loginSessionRedisService;
        this.fileStorageService = fileStorageService;
        this.ossStsService = ossStsService;
        this.ossProperties = ossProperties;
        this.javaMailSender = javaMailSender;
        this.objectMapper = objectMapper;
    }

    @Override
    /**
     * 注册流程：
     * - 基础字段校验；
     * - 唯一性校验（邮箱/手机号/昵称）；
     * - 密码加盐 MD5 存储；
     * - 写入用户并回填审计字段。
     */
    public Result<Long> register(UserRegisterRequest request) {
        if (request == null) {
            return Result.fail("请求体不能为空");
        }
        if (!StringUtils.hasText(request.getNickName())) {
            return Result.fail("nickName不能为空");
        }
        if (request.getGender() == null || request.getGender() < 0 || request.getGender() > 2) {
            return Result.fail("gender取值必须为0/1/2");
        }
        if (!StringUtils.hasText(request.getPhone()) || !PHONE_PATTERN.matcher(request.getPhone().trim()).matches()) {
            return Result.fail("phone格式错误，必须11位数字");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            return Result.fail("password不能为空");
        }
        if (!StringUtils.hasText(request.getEmail())) {
            return Result.fail("email不能为空");
        }

        String phone = request.getPhone().trim();
        String email = request.getEmail().trim();
        String nickName = request.getNickName().trim();

        boolean emailExists = friendUserMapper.selectCount(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getEmail, email)) > 0;
        if (emailExists) {
            return Result.fail("邮箱已注册");
        }
        boolean phoneExists = friendUserMapper.selectCount(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getPhone, phone)) > 0;
        if (phoneExists) {
            return Result.fail("手机号已注册");
        }
        boolean nickExists = friendUserMapper.selectCount(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getNickName, nickName)) > 0;
        if (nickExists) {
            return Result.fail("昵称已存在");
        }

        FriendUser user = new FriendUser();
        user.setNickName(nickName);
        user.setGender(request.getGender());
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(md5WithSalt(request.getPassword()));
        user.setStatus(0);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        // 先 insert 生成 userId，再回填审计字段。
        int inserted = friendUserMapper.insert(user);
        if (inserted <= 0 || user.getUserId() == null) {
            return Result.fail("注册失败");
        }
        FriendUser auditUpdate = new FriendUser();
        auditUpdate.setUserId(user.getUserId());
        auditUpdate.setCreateBy(user.getUserId());
        auditUpdate.setUpdateBy(user.getUserId());
        auditUpdate.setUpdateTime(now);
        friendUserMapper.updateById(auditUpdate);
        return Result.ok("success", user.getUserId());
    }

    @Override
    /** 手机号密码登录。 */
    public Result<Map<String, Object>> loginByPhonePassword(String phone, String password) {
        if (!StringUtils.hasText(phone)) {
            return Result.fail("phone不能为空");
        }
        if (!StringUtils.hasText(password)) {
            return Result.fail("password不能为空");
        }
        String normalizedPhone = phone.trim();
        FriendUser user = friendUserMapper.selectOne(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getPhone, normalizedPhone)
                .last("limit 1"));
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!md5WithSalt(password).equalsIgnoreCase(user.getPassword())) {
            return Result.fail("手机号或密码错误");
        }
        return issueLoginSession(user);
    }

    @Override
    /** 邮箱密码登录。 */
    public Result<Map<String, Object>> loginByEmailPassword(String email, String password) {
        if (!StringUtils.hasText(email)) {
            return Result.fail("email不能为空");
        }
        if (!StringUtils.hasText(password)) {
            return Result.fail("password不能为空");
        }
        String normalizedEmail = email.trim();
        FriendUser user = friendUserMapper.selectOne(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getEmail, normalizedEmail)
                .last("limit 1"));
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!md5WithSalt(password).equalsIgnoreCase(user.getPassword())) {
            return Result.fail("邮箱或密码错误");
        }
        return issueLoginSession(user);
    }

    @Override
    /** 按 id 获取用户详情（密码置空后返回）。 */
    public Result<FriendUser> getUserById(Long userId) {
        if (userId == null) {
            return Result.fail("userId不能为空");
        }
        FriendUser user = friendUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        // 详情返回不暴露密码。
        user.setPassword(null);
        return Result.ok("success", user);
    }

    @Override
    public Result<FriendUser> getCurrentUserDetail(String token, Long gatewayUserId) {
        if (!StringUtils.hasText(token)) {
            return Result.fail("缺少 token");
        }
        String t = token.trim();
        Long userId = resolveUserIdFromToken(t);
        if (userId == null && gatewayUserId != null && loginSessionRedisService.hasLoginSession(t)) {
            userId = gatewayUserId;
        }
        if (userId == null) {
            return Result.fail("登录态无效或已过期");
        }
        return getUserById(userId);
    }

    @Override
    public Result<String> saveMyAvatarObjectKey(String token, Long gatewayUserId, String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return Result.fail("objectKey 不能为空");
        }
        String t = token == null ? "" : token.trim();
        Long userId = resolveUserIdFromRequest(t, gatewayUserId);
        if (userId == null) {
            return Result.fail("登录态无效或已过期");
        }
        String key = objectKey.trim();
        if (!isValidAvatarObjectKeyForUser(userId, key)) {
            return Result.fail("objectKey 不合法或越权");
        }
        FriendUser exists = friendUserMapper.selectById(userId);
        if (exists == null) {
            return Result.fail("用户不存在");
        }
        FriendUser update = new FriendUser();
        update.setUserId(userId);
        update.setHeadImage(key);
        update.setUpdateBy(userId);
        update.setUpdateTime(LocalDateTime.now());
        int rows = friendUserMapper.updateById(update);
        if (rows <= 0) {
            return Result.fail("保存头像失败");
        }
        // 头像变更后清理 token -> 用户详情缓存，避免读取到旧详情。
        if (StringUtils.hasText(t)) {
            redisOperatorService.delete(LoginRedisKeys.loginTokenKey(t));
            redisOperatorService.delete(LoginRedisKeys.legacyLoginTokenKey(t));
        }
        return Result.ok("success", key);
    }

    @Override
    public Result<Map<String, String>> issueMyAvatarSts(String token, Long gatewayUserId, String dir) {
        String t = token == null ? "" : token.trim();
        Long userId = resolveUserIdFromRequest(t, gatewayUserId);
        if (userId == null) {
            return Result.fail("登录态无效或已过期");
        }
        FriendUser exists = friendUserMapper.selectById(userId);
        if (exists == null) {
            return Result.fail("用户不存在");
        }
        String prefix = buildAvatarPrefixForUser(userId, dir);
        try {
            OssStsService delegate = ossStsService;
            // 兜底：若注入的是 disabled 实现，但配置实际已启用，则按当前配置临时直连 STS。
            if (delegate instanceof DisabledOssStsServiceImpl) {
                List<String> miss = validateStsConfigMissingItems();
                if (!miss.isEmpty()) {
                    return Result.fail("申请 STS 失败: 配置未生效/缺失 -> " + String.join(", ", miss));
                }
                delegate = new AliyunOssStsServiceImpl(ossProperties);
            }
            return Result.ok("success", delegate.issueForPrefix(prefix));
        } catch (Exception e) {
            return Result.fail("申请 STS 失败: " + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, String>> getMyAvatar(String token, Long gatewayUserId) {
        if (!StringUtils.hasText(token)) {
            return Result.fail("缺少 token");
        }
        String t = token.trim();
        Long userId = resolveUserIdFromRequest(t, gatewayUserId);
        if (userId == null) {
            return Result.fail("登录态无效或已过期");
        }
        FriendUser user = friendUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        String objectKey = user.getHeadImage();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("objectKey", objectKey == null ? "" : objectKey);
        String url = "";
        if (StringUtils.hasText(objectKey)) {
            String key = objectKey.trim();
            // 优先返回签名 URL，兼容私有读 Bucket。
            url = fileStorageService.buildSignedUrl(key, 600);
            if (!StringUtils.hasText(url)) {
                url = fileStorageService.buildUrl(key);
            }
        }
        data.put("url", url);
        return Result.ok("success", data);
    }

    private Long resolveUserIdFromRequest(String token, Long gatewayUserId) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Long userId = resolveUserIdFromToken(token);
        if (userId == null && gatewayUserId != null && loginSessionRedisService.hasLoginSession(token)) {
            userId = gatewayUserId;
        }
        return userId;
    }

    private String buildAvatarPrefixForUser(Long userId, String dir) {
        String base = normalizeDir(ossProperties.getBaseDir());
        String avatarRoot = base + "avatar/" + userId + "/";
        String sub = sanitizeDirSegment(dir);
        if (!StringUtils.hasText(sub)) {
            return avatarRoot;
        }
        return avatarRoot + sub + "/";
    }

    private boolean isValidAvatarObjectKeyForUser(Long userId, String objectKey) {
        if (!StringUtils.hasText(objectKey) || objectKey.length() > 512) {
            return false;
        }
        if (objectKey.contains("..") || objectKey.contains("\\") || objectKey.startsWith("/")) {
            return false;
        }
        String userPrefix = normalizeDir(ossProperties.getBaseDir()) + "avatar/" + userId + "/";
        if (!objectKey.startsWith(userPrefix)) {
            return false;
        }
        return objectKey.matches("^[A-Za-z0-9_./-]+$");
    }

    private static String sanitizeDirSegment(String dir) {
        if (!StringUtils.hasText(dir)) {
            return "";
        }
        String s = dir.trim().replace("\\", "/");
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        if (!StringUtils.hasText(s)) {
            return "";
        }
        if (!s.matches("^[A-Za-z0-9_/-]+$")) {
            return "";
        }
        return s;
    }

    private static String normalizeDir(String dir) {
        if (!StringUtils.hasText(dir)) {
            return "";
        }
        String s = dir.trim().replace("\\", "/");
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }

    private List<String> validateStsConfigMissingItems() {
        List<String> miss = new ArrayList<>();
        if (!ossProperties.isEnabled()) {
            miss.add("oss.enabled=true");
        }
        if (!StringUtils.hasText(ossProperties.getStsRoleArn())) {
            miss.add("oss.sts-role-arn");
        }
        if (!StringUtils.hasText(ossProperties.getAccessKeyId())) {
            miss.add("oss.access-key-id");
        }
        if (!StringUtils.hasText(ossProperties.getAccessKeySecret())) {
            miss.add("oss.access-key-secret");
        }
        if (!StringUtils.hasText(ossProperties.getBucketName())) {
            miss.add("oss.bucket-name");
        }
        if (!StringUtils.hasText(ossProperties.getEndpoint())) {
            miss.add("oss.endpoint");
        }
        return miss;
    }

    @Override
    /**
     * 登出流程：
     * 1) 根据 userId 找 active token；
     * 2) 删除 token 映射与 active 映射；
     * 3) 将用户状态改为登出（0）。
     */
    public Result<Void> logout(Long userId) {
        if (userId == null) {
            return Result.fail("userId不能为空");
        }
        FriendUser user = friendUserMapper.selectById(userId);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        loginSessionRedisService.invalidateByUserId(String.valueOf(userId));
        purgeLegacyFriendLoginKeys(userId);

        FriendUser update = new FriendUser();
        update.setUserId(userId);
        update.setStatus(0);
        update.setUpdateBy(userId);
        update.setUpdateTime(LocalDateTime.now());
        friendUserMapper.updateById(update);
        return Result.ok("success");
    }

    @Override
    /**
     * 发送验证码：
     * - 校验邮箱存在；
     * - 生成 6 位验证码并写 Redis（59 秒）；
     * - 通过 SMTP 发邮件。
     */
    public Result<Void> sendEmailCode(String email) {
        if (!StringUtils.hasText(email)) {
            return Result.fail("email不能为空");
        }
        String normalizedEmail = email.trim();
        FriendUser user = friendUserMapper.selectOne(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getEmail, normalizedEmail)
                .last("limit 1"));
        if (user == null) {
            return Result.fail("用户不存在");
        }
        if (!StringUtils.hasText(mailFrom)) {
            return Result.fail("SMTP 发件邮箱未配置");
        }

        String code = VerificationCodeUtils.generateSixDigitCode();
        redisOperatorService.set(buildCodeKey(normalizedEmail), code, Duration.ofSeconds(CODE_TTL_SECONDS));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(normalizedEmail);
        message.setSubject("OJ 登录验证码");
        message.setText("您的验证码是：" + code + "，59秒内有效。");
        javaMailSender.send(message);
        return Result.ok("success");
    }

    @Override
    /**
     * 邮箱验证码登录：
     * - 校验验证码；
     * - 登录态写入 Redis（包含顶号处理）；
     * - 使用后删除验证码。
     */
    public Result<String> loginByEmailCode(String email, String code) {
        if (!StringUtils.hasText(email)) {
            return Result.fail("email不能为空");
        }
        if (!StringUtils.hasText(code)) {
            return Result.fail("验证码不能为空");
        }

        String normalizedEmail = email.trim();
        String cacheCode = redisOperatorService.get(buildCodeKey(normalizedEmail));
        if (!StringUtils.hasText(cacheCode)) {
            return Result.fail("验证码不存在或已过期");
        }
        if (!cacheCode.equals(code.trim())) {
            return Result.fail("验证码错误");
        }

        FriendUser user = friendUserMapper.selectOne(Wrappers.<FriendUser>lambdaQuery()
                .eq(FriendUser::getEmail, normalizedEmail)
                .last("limit 1"));
        if (user == null) {
            return Result.fail("用户不存在");
        }

        Result<Map<String, Object>> loginResult = issueLoginSession(user);
        redisOperatorService.delete(buildCodeKey(normalizedEmail));
        if (loginResult.getCode() != 1000 || loginResult.getData() == null) {
            return Result.fail(loginResult.getMsg());
        }
        Object token = loginResult.getData().get("token");
        if (token == null) {
            return Result.fail("登录失败");
        }
        return Result.ok("success", String.valueOf(token));
    }

    /** 组装 token 对应的用户信息负载，存入 Redis。 */
    private Map<String, Object> buildUserPayload(FriendUser user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", String.valueOf(user.getUserId()));
        payload.put("nickName", user.getNickName());
        payload.put("email", user.getEmail());
        payload.put("status", user.getStatus());
        return payload;
    }

    /** 生成邮箱验证码缓存 key。 */
    private String buildCodeKey(String email) {
        return FriendRedisKeys.loginCodeKey(email);
    }

    /**
     * 清理历史 friend 前缀登录键（迁移到与网关统一的 {@link LoginRedisKeys}）。
     */
    private void purgeLegacyFriendLoginKeys(Long userId) {
        if (userId == null) {
            return;
        }
        String activeKey = FriendRedisKeys.loginActiveKey(String.valueOf(userId));
        String oldToken = redisOperatorService.get(activeKey);
        if (StringUtils.hasText(oldToken)) {
            redisOperatorService.delete(FriendRedisKeys.loginTokenKey(oldToken));
        }
        redisOperatorService.delete(activeKey);
    }

    /**
     * 发放登录态：与网关一致，使用 {@link LoginSessionRedisService}
     * 写入 {@code LoginSessionPayloadByToken-{token}} 与 {@code ActiveLoginTokenByUserId-{userId}}，过期时间由 {@code security.token.ttl-seconds} 控制（默认 12 小时）。
     */
    private Result<Map<String, Object>> issueLoginSession(FriendUser user) {
        FriendUser update = new FriendUser();
        update.setUserId(user.getUserId());
        update.setStatus(1);
        update.setUpdateBy(user.getUserId());
        update.setUpdateTime(LocalDateTime.now());
        friendUserMapper.updateById(update);
        user.setStatus(1);

        Long userId = user.getUserId();
        purgeLegacyFriendLoginKeys(userId);

        String token = JwtTokenUtils.generateToken(jwtSecret, jwtExpireSeconds);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(buildUserPayload(user));
        } catch (JsonProcessingException e) {
            return Result.fail("用户信息序列化失败");
        }

        loginSessionRedisService.saveOrReplaceSession(
                userId,
                token,
                payloadJson,
                Duration.ofSeconds(loginSessionTtlSeconds)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", token);
        response.put("userId", String.valueOf(user.getUserId()));
        response.put("nickName", user.getNickName());
        response.put("email", user.getEmail());
        response.put("status", user.getStatus());
        return Result.ok("success", response);
    }

    /** 口令加盐 MD5，保持与系统侧一致。 */
    private String md5WithSalt(String rawPwd) {
        String text = rawPwd + (passwordSalt == null ? "" : passwordSalt);
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 从 Redis {@code LoginSessionPayloadByToken-{token}} 解析 userId，并校验 active token。
     */
    private Long resolveUserIdFromToken(String token) {
        String payloadJson = loginSessionRedisService.getLoginPayload(token);
        if (!StringUtils.hasText(payloadJson)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            JsonNode uid = root.get("userId");
            if (uid == null || uid.isNull()) {
                return null;
            }
            String userIdStr = uid.asText();
            if (!StringUtils.hasText(userIdStr)) {
                return null;
            }
            String uidTrim = userIdStr.trim();
            if (!loginSessionRedisService.matchesActiveToken(uidTrim, token)) {
                return null;
            }
            return Long.parseLong(uidTrim);
        } catch (JsonProcessingException | NumberFormatException e) {
            return null;
        }
    }
}

