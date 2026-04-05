package com.bite.friend.domain.dto;

import lombok.Data;

/** 手机号密码登录请求体。 */
@Data
public class PhonePasswordLoginRequest {
    private String phone;
    private String password;
}

