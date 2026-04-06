package com.bite.friend.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.List;

/**
 * 返回前端的判题汇总。
 */
@Data
public class CodeSubmitResultVo {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long questionId;
    /** 0 通过；1 失败；2 中间态（仅异常中断时可能见到） */
    private Integer status;
    private Integer score;
    private String exeMessage;
    private String questionCase;

    @Data
    public static class ResultVo {
        private int verdict;
        private String message;
        private String actualOutput;
        private String expectedOutput;
    }
}
