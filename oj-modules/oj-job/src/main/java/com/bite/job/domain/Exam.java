package com.bite.job.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bite.domain.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 与 oj-system 中 {@code tb_exam} 表结构一致，供 job 服务独立访问库表。
 */
@TableName("tb_exam")
@Data
public class Exam extends BaseEntity {

    @TableId(value = "exam_id", type = IdType.ASSIGN_ID)
    private Long examId;

    @TableField("title")
    private String title;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;
}
