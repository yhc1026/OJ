package com.bite.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端用户表实体，对应：tb_sys_user。
 *
 * <pre>
 * CREATE TABLE `tb_sys_user` (
 *  `user_id` bigint(20) unsigned NOT NULL COMMENT '用户id',
 *  `user_account` varchar(32) DEFAULT NULL COMMENT '用户账号',
 *  `password` varchar(100) DEFAULT NULL COMMENT '用户密码',
 *  `nick_name` varchar(32) DEFAULT NULL COMMENT '昵称',
 *  `create_by` bigint(8) NOT NULL COMMENT '创建用户',
 *  `create_time` datetime NOT NULL COMMENT '创建时间',
 *  `update_by` bigint(8) DEFAULT NULL COMMENT '更新用户',
 *  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
 *  PRIMARY KEY (`user_id`),
 *  UNIQUE KEY `user_account` (`user_account`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理端用户表'
 * </pre>
 */
@TableName("tb_sys_user")
@Data
public class SysUser extends BaseEntity {
    /**
     * 用户 id（bigint unsigned）。
     * <p>
     * 表结构未声明自增，默认按业务侧手动赋值。
     */
    @TableId(value = "user_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @TableField("user_account")
    private String userAccount;

    @TableField("password")
    private String password;

    @TableField("nick_name")
    private String nickName;
}

