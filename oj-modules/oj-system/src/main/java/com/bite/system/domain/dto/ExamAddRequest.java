package com.bite.system.domain.dto;

import com.bite.system.domain.Exam;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

/**
 * 新增竞赛请求体：包含竞赛信息 + 题目 id 列表。
 */
@Data
public class ExamAddRequest {

    /** 竞赛基础信息 */
    private Exam exam;

    /**
     * 题目 id 列表。
     * 兼容前端字段名：questionList（内容为 id 数组）。
     */
    @JsonAlias({"questionList"})
    private List<Long> questionIdList;
}

