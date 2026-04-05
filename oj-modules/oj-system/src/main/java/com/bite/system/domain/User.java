package com.bite.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * C 端用户表实体，对应：tb_user。
 */
@TableName("tb_user")
@Data
public class User extends BaseEntity {

    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @TableField("nick_name")
    private String nickName;

    /** 性别：0-未知，1-男，2-女 */
    @TableField("gender")
    private Integer gender;

    @TableField("phone")
    private String phone;

    @TableField("password")
    private String password;

    @TableField("email")
    private String email;

    @TableField("wechat")
    private String wechat;

    @TableField("school")
    private String school;

    @TableField("introduction")
    private String introduction;

    @TableField("head_image")
    private String headImage;

    /** 状态：0-禁用，1-正常 */
    @TableField("status")
    private Integer status;

    /** 性别展示文案（不落库） */
    @TableField(exist = false)
    private String genderLabel;

    /** 状态展示文案（不落库） */
    @TableField(exist = false)
    private String statusLabel;
}

