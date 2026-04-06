package com.bite.friend.domain.dto;

import lombok.Data;

/**
 * 前端提交代码判题。
 */
@Data
public class CodeSubmitRequest {
    private Long questionId;
    /** 用户源码 */
    private String code;
    /** 0 = Java，默认 Java */
    private Integer language;
    /** 可选：考试场景 */
    private Long examId;
}
