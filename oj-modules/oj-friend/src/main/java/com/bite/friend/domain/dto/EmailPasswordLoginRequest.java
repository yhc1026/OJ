package com.bite.friend.domain.dto;

import lombok.Data;

/** 邮箱密码登录请求体。 */
@Data
public class EmailPasswordLoginRequest {
    private String email;
    private String password;
}

