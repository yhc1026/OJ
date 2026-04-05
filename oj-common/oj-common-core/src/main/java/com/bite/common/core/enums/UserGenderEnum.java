package com.bite.common.core.enums;

/**
 * C 端用户性别映射：
 * 0-未知，1-男，2-女。
 */
public enum UserGenderEnum {
    UNKNOWN(0, "unknown"),
    MALE(1, "male"),
    FEMALE(2, "female");

    private final int code;
    private final String label;

    UserGenderEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static UserGenderEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserGenderEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        UserGenderEnum e = fromCode(code);
        return e == null ? "unknown" : e.label;
    }
}

