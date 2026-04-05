package com.bite.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.bite.domain.Result;
import com.bite.system.domain.SysUser;

import java.util.Map;

/**
 * 管理端用户（SysUser）业务服务层。
 * <p>
 * Controller 建议只做参数接收与返回，业务逻辑沉到 Service，便于复用与扩展事务/校验。
 */
public interface SysUserService extends IService<SysUser> {
    /**
     * 登录接口：前端传 userAccount + pwd，后端校验并返回 Result。
     *
     * @param userAccount 用户账号（tb_sys_user.user_account）
     * @param pwd         前端传入密码（演示：与表中 password 做字符串比对）
     */
    Result<Map<String, Object>> login(String userAccount, String pwd);

    /**
     * 新增用户：createBy / updateBy 不从 body 取，由请求头 {@code X-User-Id}（可配置）传入当前操作人 id。
     */
    Result<Boolean> insertUser(SysUser body);

    /**
     * 退出登录：根据当前用户 id 在 Redis 中查找 {@code ActiveLoginTokenByUserId-{userId}} 对应的活动 token，
     * 删除 {@code LoginSessionPayloadByToken-{token}} 与活动指针；并清理当前请求令牌在 Redis 中的残留。
     */
    Result<Void> logout();
}

