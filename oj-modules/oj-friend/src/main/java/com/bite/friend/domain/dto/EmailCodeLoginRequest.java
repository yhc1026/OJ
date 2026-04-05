package com.bite.friend.domain.dto;

import lombok.Data;

/** 邮箱验证码登录请求体。 */
@Data
public class EmailCodeLoginRequest {
    private String email;
    private String code;
}

