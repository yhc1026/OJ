package com.bite.judge.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tb_judge_mq_consume_log")
public class JudgeMqConsumeLog {

    @TableId("message_id")
    private String messageId;

    @TableField("submit_id")
    private Long submitId;

    @TableField("consume_status")
    private Integer consumeStatus;

    @TableField("last_error")
    private String lastError;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
