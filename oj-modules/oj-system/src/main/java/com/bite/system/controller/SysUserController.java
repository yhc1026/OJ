package com.bite.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bite.common.core.enums.ResultCode;
import com.bite.domain.Result;
import com.bite.system.domain.SysUser;
import com.bite.system.service.SysUserService;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 管理端用户（tb_sys_user）CRUD 接口（基于 MyBatis-Plus）。
 */
@RequestMapping("/sysUser")
@RestController
public class SysUserController {
    /** 用户列表每页条数（MyBatis-Plus 分页） */
    private static final int SYS_USER_PAGE_SIZE = 20;

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 分页查询（MyBatis-Plus {@link Page}，每页 {@link #SYS_USER_PAGE_SIZE} 条）。
     * <p>
     * 未使用 PageHelper：其与 MP 3.5.3.x 依赖的 JSQLParser 版本在运行时易冲突（NoClassDefFoundError / VerifyError）。
     *
     * @param page 页码，从 1 开始
     */
    @GetMapping("/sys-users")
    public Result<IPage<SysUser>> list(@RequestParam(value = "page", defaultValue = "1") long page) {
        long current = Math.max(1L, page);
        IPage<SysUser> data = sysUserService.page(new Page<>(current, SYS_USER_PAGE_SIZE));
        return Result.ok(ResultCode.SUCCESS.getMsg(), data);
    }

    /**
     * 按用户 id 查询。
     * URL 按要求：findUserById
     */
    @GetMapping("/findUserById")
    public Result<SysUser> findUserById(@RequestParam("id") Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), user);
    }

    /**
     * 按用户账号查询（user_account 唯一）。
     * URL 按要求：findUserByUserAccount
     */
    @GetMapping("/findUserByUserAccount")
    public Result<SysUser> findUserByUserAccount(@RequestParam("userAccount") String userAccount) {
        Assert.hasText(userAccount, "userAccount must not be blank");
        SysUser user = sysUserService.getOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserAccount, userAccount)
        );
        if (user == null) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), user);
    }

    /**
     * 仅根据 user_account 查询 user_id。
     */
    @GetMapping("/findUserIdByUserAccount")
    public Result<Long> findUserIdByUserAccount(@RequestParam("userAccount") String userAccount) {
        Assert.hasText(userAccount, "userAccount must not be blank");
        SysUser user = sysUserService.getOne(
                Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getUserId)
                        .eq(SysUser::getUserAccount, userAccount)
        );
        if (user == null || user.getUserId() == null) {
            return Result.fail(ResultCode.FAILED_USER_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), user.getUserId());
    }

    /**
     * 新增用户。
     * <p>
     * 注意：表结构未声明自增，默认要求请求体携带 userId。
     */
    @PostMapping("/insertUser")
    public Result<Boolean> insertUser(@RequestBody SysUser body) {
        // createBy / updateBy 由 Service 从请求头 X-User-Id 解析（网关鉴权通过后会自动注入）
        return sysUserService.insertUser(body);
    }

    /**
     * 按主键更新用户（请求体需携带 userId）。
     */
    @PutMapping("/updateUserById")
    public Result<Boolean> updateUserById(@RequestBody SysUser body) {
        if (body != null && body.getPassword() != null && !body.getPassword().isBlank()) {
            body.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex(
                    (body.getPassword() + System.getProperty("PWD_SALT", "oj-salt"))
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));
        }
        if (body != null) {
            touchUpdateAudit(body);
        }
        boolean ok = sysUserService.updateById(body);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    /**
     * 按 user_account 更新用户（user_account 唯一）。
     * URL 按要求：updateUserByUserAccount
     *
     * 注意：请求体需要携带 userAccount 作为更新条件；其余非空字段会被更新。
     */
    @PutMapping("/updateUserByUserAccount")
    public Result<Boolean> updateUserByUserAccount(@RequestBody SysUser body) {
        Assert.notNull(body, "body must not be null");
        Assert.hasText(body.getUserAccount(), "userAccount must not be blank");
        if (body.getPassword() != null && !body.getPassword().isBlank()) {
            body.setPassword(org.springframework.util.DigestUtils.md5DigestAsHex(
                    (body.getPassword() + System.getProperty("PWD_SALT", "oj-salt"))
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));
        }
        touchUpdateAudit(body);
        boolean ok = sysUserService.update(
                body,
                Wrappers.<SysUser>lambdaUpdate().eq(SysUser::getUserAccount, body.getUserAccount())
        );
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    /**
     * 登录接口（演示用）。
     * <p>
     * 前端传参：userAccount + pwd
     * 返回：Result（msg + code）
     */
    @GetMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam("userAccount") String userAccount,
                                             @RequestParam("pwd") String pwd) {
        return sysUserService.login(userAccount, pwd);
    }

    /**
     * 退出登录：需携带与登录一致的 token（及经网关时的 {@code X-User-Id}）。
     * <p>
     * 后端根据当前用户 id 在 Redis 中查找 {@code ActiveLoginTokenByUserId-{userId}} 对应的活动 JWT，
     * 删除 {@code LoginSessionPayloadByToken-{jwt}} 与活动指针，使网关后续校验失败。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return sysUserService.logout();
    }

    /**
     * 按用户 id 删除。
     * URL 按要求：/deleteUserById
     */
    @DeleteMapping("/deleteUserById")
    public Result<Boolean> deleteUserById(@RequestParam("id") Long id) {
        boolean ok = sysUserService.removeById(id);
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    /**
     * 按 user_account 删除（user_account 唯一）。
     * URL 按命名风格：deleteUserByUserAccount
     */
    @DeleteMapping("/deleteUserByUserAccount")
    public Result<Boolean> deleteUserByUserAccount(@RequestParam("userAccount") String userAccount) {
        Assert.hasText(userAccount, "userAccount must not be blank");
        boolean ok = sysUserService.remove(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserAccount, userAccount)
        );
        if (!ok) {
            return Result.fail(ResultCode.FAILED_NOT_EXISTS);
        }
        return Result.ok(ResultCode.SUCCESS.getMsg(), true);
    }

    /** 更新时写入更新时间；操作人未传则用 0（与 insert 约定一致） */
    private static void touchUpdateAudit(SysUser body) {
        body.setUpdateTime(LocalDateTime.now());
        if (body.getUpdateBy() == null) {
            body.setUpdateBy(0L);
        }
    }
}

