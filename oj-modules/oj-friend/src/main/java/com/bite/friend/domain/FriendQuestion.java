package com.bite.friend.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目实体（tb_question）。
 * 用于回源后把题目详情序列化为 JSON 缓存到 Redis。
 */
@TableName("tb_question")
@Data
public class FriendQuestion {
    @TableId("question_id")
    private Long questionId;

    @TableField("title")
    private String title;

    @TableField("difficulty")
    private Integer difficulty;

    @TableField("time_limit")
    private Long timeLimit;

    @TableField("space_limit")
    private Long spaceLimit;

    @TableField("content")
    private String content;

    @TableField("question_case")
    private String questionCase;

    @TableField("default_code")
    private String defaultCode;

    @TableField("main_method")
    private String mainMethod;

    @TableField("expected_result")
    private String expectedResult;
}

