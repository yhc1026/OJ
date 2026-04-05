package com.bite.friend.domain.dto;

import com.bite.friend.json.FlexibleLongDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

/**
 * 用户报名竞赛请求：携带目标竞赛 id。
 * <p>
 * JSON 中 {@code examId} 建议传<strong>字符串</strong>（雪花 id），与前端一致；仍兼容 JSON 数字（如 Postman）。
 */
@Data
public class ExamRegisterRequest {

    /** 要报名的竞赛 id */
    @JsonDeserialize(using = FlexibleLongDeserializer.class)
    private Long examId;
}
