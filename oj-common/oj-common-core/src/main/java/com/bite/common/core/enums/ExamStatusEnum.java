package com.bite.common.core.enums;

/**
 * 竞赛状态映射。
 * 0-未开始，1-进行中，2-已结束
 */
public enum ExamStatusEnum {
    NOT_STARTED(0, "not_started"),
    RUNNING(1, "running"),
    FINISHED(2, "finished");

    private final int code;
    private final String label;

    ExamStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(Integer code) {
        if (code == null) {
            return "unknown";
        }
        for (ExamStatusEnum value : values()) {
            if (value.code == code) {
                return value.label;
            }
        }
        return "unknown";
    }
}

