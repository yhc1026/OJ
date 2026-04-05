package com.bite.common.core.enums;

/**
 * 题目难度：数据库存储为 tinyint，与展示用英文标签映射。
 * <ul>
 *   <li>0 — easy</li>
 *   <li>1 — medium</li>
 *   <li>2 — hard</li>
 * </ul>
 */
public enum QuestionDifficultyEnum {

    EASY(0, "easy"),
    MEDIUM(1, "medium"),
    HARD(2, "hard");

    private final int code;
    private final String label;

    QuestionDifficultyEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 根据库中难度数值解析枚举；无法识别时返回 {@code null}。
     *
     * @param code 库中难度，可为 {@code null}
     */
    public static QuestionDifficultyEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (QuestionDifficultyEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    /**
     * 根据难度英文标签解析枚举（忽略大小写和首尾空白）；无法识别时返回 {@code null}。
     */
    public static QuestionDifficultyEnum fromLabel(String label) {
        if (label == null) {
            return null;
        }
        String value = label.trim();
        if (value.isEmpty()) {
            return null;
        }
        for (QuestionDifficultyEnum e : values()) {
            if (e.label.equalsIgnoreCase(value)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 返回与 {@link #fromCode(Integer)} 对应的展示字符串；未映射时返回 {@code "unknown"}。
     *
     * @param code 库中难度，可为 {@code null}（此时返回 {@code "unknown"}）
     */
    public static String labelOf(Integer code) {
        if (code == null) {
            return "unknown";
        }
        QuestionDifficultyEnum e = fromCode(code);
        return e != null ? e.label : "unknown";
    }
}
