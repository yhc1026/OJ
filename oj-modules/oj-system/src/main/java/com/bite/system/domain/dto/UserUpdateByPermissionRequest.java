package com.bite.system.domain.dto;

import com.bite.system.domain.User;
import lombok.Data;

/**
 * 统一编辑普通用户入参：
 * 1) targetUserId：目标用户 id；
 * 2) content：要修改的字段（按 User 可更新字段填写）。
 */
@Data
public class UserUpdateByPermissionRequest {

    private Long targetUserId;

    private User content;
}

