package com.bite.system.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 题目概要（分页列表 / 按 id 简要查询）：标题、难度、时间与空间限制。
 */
@Data
@NoArgsConstructor
public class QuestionBriefVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;
    private String title;
    /** 库中 tinyint 难度值 */
    private Integer difficulty;
    /** 难度展示：easy / medium / hard，未在枚举内则为 unknown */
    private String difficultyLabel;
    /** 时间限制，可为 null */
    private Integer timeLimit;
    /** 空间限制，可为 null */
    private Integer spaceLimit;
}
