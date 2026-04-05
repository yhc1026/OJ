package com.bite.friend.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * C 端竞赛列表响应对象。
 * <p>
 * {@code examId} / {@code questionIds} 在 JSON 中序列化为字符串，避免超过 JS
 * {@code Number.MAX_SAFE_INTEGER}（2^53-1）的雪花 id 在浏览器 {@code JSON.parse} 后精度丢失，
 * 导致前端请求详情时 examId 错误、接口返回「资源不存在」。
 */
@Data
public class ExamResponse {
    /** 竞赛主键；列表类接口可选填充。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long examId;
    private String title;
    /**
     * 使用枚举映射后的字符串状态，如 not_started/running/finished。
     */
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * 仅 {@code /friend/exam/detail} 填充：该场竞赛关联的题目 id（顺序与题库一致），用于前端拉取 {@code /question/detail}。
     */
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> questionIds;
}

