package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代码提交记录（tb_user_submit）。
 *
 * <pre>
 * CREATE TABLE tb_user_submit (
 *   submit_id bigint NOT NULL COMMENT '提交id',
 *   user_id bigint NOT NULL,
 *   question_id bigint NOT NULL,
 *   exam_id bigint COMMENT '考试id，可为空',
 *   user_code text NOT NULL,
 *   language tinyint NOT NULL COMMENT '0=Java',
 *   exe_message varchar(1024) COMMENT '执行/判题信息',
 *   score int NOT NULL DEFAULT 0,
 *   create_by bigint NOT NULL,
 *   create_time datetime NOT NULL,
 *   update_by bigint,
 *   update_time datetime,
 *   status tinyint NOT NULL COMMENT '0通过 1失败 2中间态',
 *   PRIMARY KEY (submit_id)
 * );
 * </pre>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("tb_user_submit")
public class FriendCodeSubmit extends BaseEntity {

    @TableId(value = "submit_id", type = IdType.ASSIGN_ID)
    private Long submitId;

    @TableField("user_id")
    private Long userId;

    @TableField("question_id")
    private Long questionId;

    @TableField("exam_id")
    private Long examId;

    @TableField("user_code")
    private String userCode;

    /** 0 = Java */
    @TableField("language")
    private Integer language;

    @TableField("exe_message")
    private String exeMessage;

    @TableField("score")
    private Integer score;

    /** 0 通过；1 失败；2 中间态（已落库，判题未完成） */
    @TableField("status")
    private Integer status;
}
