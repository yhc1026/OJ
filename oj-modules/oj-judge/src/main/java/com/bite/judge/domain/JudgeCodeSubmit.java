package com.bite.judge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tb_user_submit")
public class JudgeCodeSubmit extends BaseEntity {

    @TableId(value = "submit_id", type = IdType.ASSIGN_ID)
    private Long submitId;

    @TableField("score")
    private Integer score;

    @TableField("status")
    private Integer status;

    @TableField("exe_message")
    private String exeMessage;
}
