package com.bite.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bite.common.core.enums.ResultCode;
import com.bite.common.core.enums.UserGenderEnum;
import com.bite.common.core.enums.UserStatusEnum;
import com.bite.common.redis.session.LoginSessionRedisService;
import com.bite.domain.Result;
import com.bite.system.domain.SysUser;
import com.bite.system.domain.User;
import com.bite.system.mapper.SysUserMapper;
import com.bite.system.mapper.UserMapper;
import com.bite.system.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final int USER_PAGE_SIZE = 20;
    private static final String ROOT_ACCOUNT = "root";
    @Value("${security.password.salt:oj-salt}")
    private String passwordSalt;

    private final LoginSessionRedisService loginSessionRedisService;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    public UserServiceImpl(LoginSessionRedisService loginSessionRedisService,
                           SysUserMapper sysUserMapper,
                           ObjectMapper objectMapper) {
        this.loginSessionRedisService = loginSessionRedisService;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<IPage<User>> list(long page) {
        long current = Math.max(1L, page);
        IPage<User> raw = page(new Page<>(current, USER_PAGE_SIZE));
        IPage<User> converted = raw.convert(this::toView);
        return Result.ok(ResultCode.SUCCESS.getMsg(), converted);
    }

    @Override
    public Result<User> getUserById(Long userId) {
        if (userId == null) {
            return new Result<>("userId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        User user = getById(userId);
        if (user == null) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toView(user));
    }

    @Override
    public Result<User> getUserByName(String name) {
        if (!StringUtils.hasText(name)) {
            return new Result<>("name 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        List<User> users = list(Wrappers.<User>lambdaQuery().eq(User::getNickName, name.trim()));
        if (users.isEmpty()) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (users.size() > 1) {
            return new Result<>("存在同名用户，请使用 userId 查询", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), toView(users.get(0)));
    }

    @Override
    public Result<Long> addUser(User body) {
        if (body == null) {
            return new Result<>("请求体不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (body.getUserId() != null) {
            return new Result<>("userId 由系统雪花算法生成，请勿手动传入", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String err = validateUserForCreate(body);
        if (err != null) {
            return new Result<>(err, ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        long sameName = count(Wrappers.<User>lambdaQuery().eq(User::getNickName, body.getNickName().trim()));
        if (sameName > 0) {
            return new Result<>("用户昵称已存在，不可重复", ResultCode.FAILED_ALREADY_EXISTS.getCode(), null);
        }
        body.setNickName(body.getNickName().trim());
        LocalDateTime now = LocalDateTime.now();
        if (body.getCreateBy() == null) {
            body.setCreateBy(0L);
        }
        if (body.getUpdateBy() == null) {
            body.setUpdateBy(body.getCreateBy());
        }
        body.setCreateTime(now);
        body.setUpdateTime(now);
        boolean ok = save(body);
        if (!ok || body.getUserId() == null) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), body.getUserId());
    }

    @Override
    public Result<Boolean> deleteUserById(Long userId) {
        if (userId == null) {
            return new Result<>("userId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        boolean ok = removeById(userId);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    @Override
    public Result<Integer> deleteUserByName(String name) {
        if (!StringUtils.hasText(name)) {
            return new Result<>("name 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String nick = name.trim();
        long cnt = count(Wrappers.<User>lambdaQuery().eq(User::getNickName, nick));
        if (cnt == 0) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        boolean ok = remove(Wrappers.<User>lambdaQuery().eq(User::getNickName, nick));
        if (!ok) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), (int) cnt);
    }

    @Override
    public Result<Boolean> updateUserById(User body) {
        if (body == null || body.getUserId() == null) {
            return new Result<>("更新时 userId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String err = validateUserForUpdate(body);
        if (err != null) {
            return new Result<>(err, ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (StringUtils.hasText(body.getNickName())) {
            String nick = body.getNickName().trim();
            long dup = count(
                    Wrappers.<User>lambdaQuery()
                            .eq(User::getNickName, nick)
                            .ne(User::getUserId, body.getUserId())
            );
            if (dup > 0) {
                return new Result<>("用户昵称已存在，不可重复", ResultCode.FAILED_ALREADY_EXISTS.getCode(), null);
            }
            body.setNickName(nick);
        }
        if (body.getUpdateBy() == null) {
            body.setUpdateBy(0L);
        }
        body.setUpdateTime(LocalDateTime.now());
        boolean ok = updateById(body);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    @Override
    public Result<Boolean> updateUserByPermission(Long targetUserId, User content) {
        if (targetUserId == null) {
            return new Result<>("targetUserId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        if (content == null) {
            return new Result<>("content 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        //请求用户的身份标签
        TokenPrincipal principal = resolvePrincipalFromRequest();
        if (principal == null) {
            return new Result<>("未登录或登录态已失效", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        SysUser targetSys = sysUserMapper.selectById(targetUserId);

        // 1) root: 可改 sysuser 或 user
        if (principal.isRoot) {
            if (targetSys != null) {
                return updateSysUserByRoot(targetUserId, content, principal.userId);
            }
            User exists = getById(targetUserId);
            if (exists == null) {
                return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
            }
            content.setUserId(targetUserId);
            content.setUpdateBy(principal.userId);
            return updateUserById(content);
        }

        // 2) 非 root 目标若是 sysuser，直接拦截
        if (targetSys != null) {
            return new Result<>("无权限修改系统用户信息", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }

        // 3) 普通 sysuser: 可改所有普通 user；普通 user: 仅可改自己
        if (!principal.isSysUser && !principal.userId.equals(targetUserId)) {
            return new Result<>("无权限修改该用户信息", ResultCode.FAILED_UNAUTHORIZED.getCode(), null);
        }
        User exists = getById(targetUserId);
        if (exists == null) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        content.setUserId(targetUserId);
        content.setUpdateBy(principal.userId);
        return updateUserById(content);
    }

    @Override
    public Result<Integer> updateUserByName(User body) {
        if (body == null || !StringUtils.hasText(body.getNickName())) {
            return new Result<>("更新时 name 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String err = validateUserForUpdate(body);
        if (err != null) {
            return new Result<>(err, ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        String nick = body.getNickName().trim();
        long cnt = count(Wrappers.<User>lambdaQuery().eq(User::getNickName, nick));
        if (cnt == 0) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        if (body.getUpdateBy() == null) {
            body.setUpdateBy(0L);
        }
        body.setUpdateTime(LocalDateTime.now());
        boolean ok = update(body, Wrappers.<User>lambdaUpdate().eq(User::getNickName, nick));
        if (!ok) {
            return Result.fail(ResultCode.FAILED);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), (int) cnt);
    }

    @Override
    public Result<Boolean> banUserById(Long userId) {
        if (userId == null) {
            return new Result<>("userId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        boolean ok = update(
                Wrappers.<User>lambdaUpdate()
                        .set(User::getStatus, UserStatusEnum.BLACKLIST.getCode())
                        .set(User::getUpdateTime, LocalDateTime.now())
                        .set(User::getUpdateBy, 0L)
                        .eq(User::getUserId, userId)
        );
        if (!ok) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    @Override
    public Result<Boolean> unbanUserById(Long userId) {
        if (userId == null) {
            return new Result<>("userId 不能为空", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        boolean ok = update(
                Wrappers.<User>lambdaUpdate()
                        .set(User::getStatus, UserStatusEnum.LOGIN.getCode())
                        .set(User::getUpdateTime, LocalDateTime.now())
                        .set(User::getUpdateBy, 0L)
                        .eq(User::getUserId, userId)
        );
        if (!ok) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    private String validateUserForCreate(User body) {
        if (!StringUtils.hasText(body.getNickName())) return "nickName 不能为空";
        if (!StringUtils.hasText(body.getPhone()) || !body.getPhone().matches("^\\d{11}$")) return "手机号必须为11位数字";
        if (!StringUtils.hasText(body.getPassword()) || body.getPassword().length() > 30) return "password 不能为空且长度不超过30";
        if (!StringUtils.hasText(body.getEmail()) || body.getEmail().length() > 50) return "email 不能为空且长度不超过50";
        if (body.getGender() != null && UserGenderEnum.fromCode(body.getGender()) == null) return "gender 仅支持 0/1/2";
        if (body.getStatus() == null || UserStatusEnum.fromCode(body.getStatus()) == null) return "status 仅支持 0/1/2";
        if (body.getNickName().trim().length() > 30) return "nickName 长度不超过30";
        if (StringUtils.hasText(body.getWechat()) && body.getWechat().length() > 40) return "wechat 长度不超过40";
        if (StringUtils.hasText(body.getSchool()) && body.getSchool().length() > 15) return "school 长度不超过15";
        if (StringUtils.hasText(body.getIntroduction()) && body.getIntroduction().length() > 100) return "introduction 长度不超过100";
        if (StringUtils.hasText(body.getHeadImage()) && body.getHeadImage().length() > 255) return "headImage 长度不超过255";
        return null;
    }

    private String validateUserForUpdate(User body) {
        if (body.getPhone() != null && !body.getPhone().matches("^\\d{11}$")) return "手机号必须为11位数字";
        if (body.getPassword() != null && body.getPassword().length() > 30) return "password 长度不超过30";
        if (body.getEmail() != null && body.getEmail().length() > 50) return "email 长度不超过50";
        if (body.getGender() != null && UserGenderEnum.fromCode(body.getGender()) == null) return "gender 仅支持 0/1/2";
        if (body.getStatus() != null && UserStatusEnum.fromCode(body.getStatus()) == null) return "status 仅支持 0/1/2";
        if (body.getNickName() != null && body.getNickName().trim().length() > 30) return "nickName 长度不超过30";
        if (body.getWechat() != null && body.getWechat().length() > 40) return "wechat 长度不超过40";
        if (body.getSchool() != null && body.getSchool().length() > 15) return "school 长度不超过15";
        if (body.getIntroduction() != null && body.getIntroduction().length() > 100) return "introduction 长度不超过100";
        if (body.getHeadImage() != null && body.getHeadImage().length() > 255) return "headImage 长度不超过255";
        return null;
    }

    private User toView(User src) {
        User u = new User();
        u.setUserId(src.getUserId());
        u.setNickName(src.getNickName());
        u.setGender(src.getGender());
        u.setGenderLabel(UserGenderEnum.labelOf(src.getGender()));
        u.setPhone(src.getPhone());
        u.setPassword(null);
        u.setEmail(src.getEmail());
        u.setWechat(src.getWechat());
        u.setSchool(src.getSchool());
        u.setIntroduction(src.getIntroduction());
        u.setHeadImage(src.getHeadImage());
        u.setStatus(src.getStatus());
        u.setStatusLabel(UserStatusEnum.labelOf(src.getStatus()));
        u.setCreateBy(src.getCreateBy());
        u.setCreateTime(src.getCreateTime());
        u.setUpdateBy(src.getUpdateBy());
        u.setUpdateTime(src.getUpdateTime());
        return u;
    }

    /**
     * root 修改 sysuser 信息：仅处理公共可映射字段（nickName/password）。
     */
    private Result<Boolean> updateSysUserByRoot(Long targetSysUserId, User content, Long operatorId) {
        SysUser update = new SysUser();
        update.setUserId(targetSysUserId);
        boolean changed = false;
        if (StringUtils.hasText(content.getNickName())) {
            update.setNickName(content.getNickName().trim());
            changed = true;
        }
        if (StringUtils.hasText(content.getPassword())) {
            String encrypted = DigestUtils.md5DigestAsHex(
                    (content.getPassword() + (passwordSalt == null ? "" : passwordSalt))
                            .getBytes(StandardCharsets.UTF_8)
            );
            update.setPassword(encrypted);
            changed = true;
        }
        if (!changed) {
            return new Result<>("修改内容为空：root 修改 sysuser 仅支持 nickName/password", ResultCode.FAILED_PARAMS_VALIDATE.getCode(), null);
        }
        update.setUpdateBy(operatorId == null ? 0L : operatorId);
        update.setUpdateTime(LocalDateTime.now());
        int rows = sysUserMapper.updateById(update);
        if (rows <= 0) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    /** 从当前请求 token 解析身份：sysUser / user。 */
    private TokenPrincipal resolvePrincipalFromRequest() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        String token = extractToken(req);
        if (!StringUtils.hasText(token)) {
            return null;
        }
        String payload = loginSessionRedisService.getLoginPayload(token);
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode uidNode = root.get("userId");
            if (uidNode == null || uidNode.isNull() || !StringUtils.hasText(uidNode.asText())) {
                return null;
            }
            String uid = uidNode.asText().trim();
            if (!loginSessionRedisService.matchesActiveToken(uid, token)) {
                return null;
            }
            long userId = Long.parseLong(uid);

            String identity = root.hasNonNull("identity") ? root.get("identity").asText() : null;
            boolean isSys = "sysUser".equals(identity);

            // 兼容：若 payload 未带 identity，但 userId 存在于 tb_sys_user，则判定为 sysUser
            if (!isSys) {
                SysUser su = sysUserMapper.selectById(userId);
                if (su != null) {
                    isSys = true;
                    identity = "sysUser";
                }
            }

            boolean isRoot = false;
            if (isSys) {
                SysUser su = sysUserMapper.selectById(userId);
                isRoot = su != null && ROOT_ACCOUNT.equalsIgnoreCase(su.getUserAccount());
            }
            return new TokenPrincipal(userId, isSys, isRoot);
        } catch (JsonProcessingException | NumberFormatException e) {
            return null;
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private static String extractToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StringUtils.hasText(token)) {
            return token.trim();
        }
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return auth.substring(7).trim();
        }
        String queryToken = request.getParameter("token");
        return StringUtils.hasText(queryToken) ? queryToken.trim() : null;
    }

    private static final class TokenPrincipal {
        private final Long userId;
        private final boolean isSysUser;
        private final boolean isRoot;

        private TokenPrincipal(Long userId, boolean isSysUser, boolean isRoot) {
            this.userId = userId;
            this.isSysUser = isSysUser;
            this.isRoot = isRoot;
        }
    }
}

