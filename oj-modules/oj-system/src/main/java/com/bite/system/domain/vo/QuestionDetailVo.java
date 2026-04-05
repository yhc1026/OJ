package com.bite.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目详情：与表 tb_question 业务字段及审计字段对齐（不含主键以外的敏感冗余）。
 */
@Data
@NoArgsConstructor
public class QuestionDetailVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;
    private String title;
    private Integer difficulty;
    /** 难度展示：easy / medium / hard，未在枚举内则为 unknown */
    private String difficultyLabel;
    private Integer timeLimit;
    private Integer spaceLimit;
    private String content;
    private String questionCase;
    private String defaultCode;
    private String mainMethod;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;
}
