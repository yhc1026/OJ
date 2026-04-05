package com.bite.utils;

import java.security.SecureRandom;

/**
 * 验证码工具：生成 6 位数字验证码。
 */
public final class VerificationCodeUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    private VerificationCodeUtils() {
    }

    public static String generateSixDigitCode() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}

