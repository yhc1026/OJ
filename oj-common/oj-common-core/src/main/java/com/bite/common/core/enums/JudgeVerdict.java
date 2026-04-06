package com.bite.common.core.enums;

/**
 * 判题结论码（与运行脚本、friend 汇总逻辑共用）。
 */
public enum JudgeVerdict {
    ACCEPTED(0),
    WRONG_ANSWER(1),
    COMPILE_ERROR(2),
    RUNTIME_ERROR(3),
    TIME_LIMIT(4),
    MEMORY_LIMIT(5),
    INTERNAL_ERROR(99);

    private final int code;

    JudgeVerdict(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
