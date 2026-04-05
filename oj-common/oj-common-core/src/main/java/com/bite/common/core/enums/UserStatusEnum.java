package com.bite.common.core.enums;

/**
 * C 端用户状态映射：
 * 0-登出，1-登录，2-进入黑名单。
 */
public enum UserStatusEnum {
    LOGOUT(0, "logout"),
    LOGIN(1, "login"),
    BLACKLIST(2, "blacklist");

    private final int code;
    private final String label;

    UserStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static UserStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatusEnum e : values()) {
            if (e.code == code) {
                return e;
            }
        }
        return null;
    }

    public static String labelOf(Integer code) {
        UserStatusEnum e = fromCode(code);
        return e == null ? "unknown" : e.label;
    }
}

