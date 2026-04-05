package com.bite.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.bite.domain.Result;
import com.bite.system.domain.User;

/**
 * C 端用户（tb_user）业务接口。
 */
public interface UserService extends IService<User> {
    Result<IPage<User>> list(long page);

    Result<User> getUserById(Long userId);

    Result<User> getUserByName(String name);

    Result<Long> addUser(User body);

    Result<Boolean> deleteUserById(Long userId);

    Result<Integer> deleteUserByName(String name);

    Result<Boolean> updateUserById(User body);

    /**
     * 统一编辑普通用户：服务层按 token 判定权限后放行更新。
     *
     * @param targetUserId 目标用户 ID
     * @param content      修改内容
     */
    Result<Boolean> updateUserByPermission(Long targetUserId, User content);

    Result<Integer> updateUserByName(User body);

    /** 按 userId 拉黑（status=2） */
    Result<Boolean> banUserById(Long userId);

    /** 按 userId 解禁（status=1） */
    Result<Boolean> unbanUserById(Long userId);
}

