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
 * 考试-题目关联表实体，对应：tb_exam_question。
 *
 * <pre>
 * exam_question_id bigint(20)
 * question_id      bigint(20)
 * exam_id          bigint(20)
 * question_order   int(11)
 * create_by        bigint(20)
 * create_time      datetime
 * update_by        bigint(20)
 * update_time      datetime
 * </pre>
 */
@TableName("tb_exam_question")
@Data
public class ExamQuestion extends BaseEntity {

    @TableId(value = "exam_question_id", type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examQuestionId;

    @TableField("question_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;

    @TableField("exam_id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;

    @TableField("question_order")
    private Integer questionOrder;
}

