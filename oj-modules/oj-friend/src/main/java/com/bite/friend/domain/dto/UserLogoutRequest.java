package com.bite.friend.domain.dto;

import lombok.Data;

/** 用户登出请求体。 */
@Data
public class UserLogoutRequest {
    private Long userId;
}

