package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 竞赛-题目关系实体（tb_exam_question）。
 * 用于把 exam 与 question 的关联关系缓存到 Redis。
 */
@TableName("tb_exam_question")
@Data
public class FriendExamQuestion {
    @TableId("exam_question_id")
    private Long examQuestionId;

    @TableField("exam_id")
    private Long examId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_order")
    private Integer questionOrder;
}

