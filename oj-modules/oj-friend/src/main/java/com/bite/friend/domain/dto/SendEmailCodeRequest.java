package com.bite.friend.domain.dto;

import lombok.Data;

/** 发送邮箱验证码请求体。 */
@Data
public class SendEmailCodeRequest {
    private String email;
}

