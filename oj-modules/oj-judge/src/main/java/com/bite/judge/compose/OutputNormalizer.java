package com.bite.judge.compose;

/**
 * 输出规范化（与比对逻辑一致）。
 */
public final class OutputNormalizer {

    private OutputNormalizer() {
    }

    public static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\r\n", "\n").replace('\r', '\n').stripTrailing().stripLeading();
    }

    public static boolean equalNormalized(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }
}
