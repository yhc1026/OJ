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
import java.util.List;

/**
 * 考试表实体，对应：tb_exam。
 *
 * <pre>
 * exam_id     bigint(20)
 * title       varchar(50)
 * start_time  datetime
 * end_time    datetime
 * status      tinyint(4)
 * create_by   bigint(20)
 * create_time datetime
 * update_by   bigint(20)
 * update_time datetime
 * </pre>
 */
@TableName("tb_exam")
@Data
public class Exam extends BaseEntity {

    @TableId(value = "exam_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    @TableField("title")
    private String title;

    @TableField("start_time")
    private LocalDateTime startTime;

    @TableField("end_time")
    private LocalDateTime endTime;

    @TableField("status")
    private Integer status;

    /**
     * 仅用于创建竞赛时接收题目 id 列表，不落库。
     */
    @TableField(exist = false)
    private List<Long> questionIdList;
}

