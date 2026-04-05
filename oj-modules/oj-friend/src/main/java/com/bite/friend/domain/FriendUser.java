package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * C 端用户实体（oj-friend 登录/注册场景）。
 */
@TableName("tb_user")
@Data
public class FriendUser {
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    private Long userId;

    @TableField("nick_name")
    private String nickName;

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

    @TableField("status")
    private Integer status;

    @TableField("create_by")
    private Long createBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

