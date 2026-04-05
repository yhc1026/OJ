package com.bite.friend.domain.dto;

import lombok.Data;

/** 用户注册请求体（必填项：昵称/性别/手机号/密码/邮箱）。 */
@Data
public class UserRegisterRequest {
    private String nickName;
    private Integer gender;
    private String phone;
    private String password;
    private String email;
}

