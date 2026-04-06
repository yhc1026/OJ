package com.bite.judge.domain.dto.friend;

import java.io.Serializable;

/**
 * 单用例判题请求（friend → judge，经 OpenFeign）。
 * <p>
 * 独立于内部 DTO，确保 Feign 反序列化时不依赖内部类结构。
 */
public class JudgeRunRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userCode;
    private String mainMethod;
    private String testInput;
    private String expectedOutput;
    private long timeLimitMs;
    private long spaceLimitKb;
    private int language;

    public JudgeRunRequest() {
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getMainMethod() {
        return mainMethod;
    }

    public void setMainMethod(String mainMethod) {
        this.mainMethod = mainMethod;
    }

    public String getTestInput() {
        return testInput;
    }

    public void setTestInput(String testInput) {
        this.testInput = testInput;
    }

    public String getExpectedOutput() {
        return expectedOutput;
    }

    public void setExpectedOutput(String expectedOutput) {
        this.expectedOutput = expectedOutput;
    }

    public long getTimeLimitMs() {
        return timeLimitMs;
    }

    public void setTimeLimitMs(long timeLimitMs) {
        this.timeLimitMs = timeLimitMs;
    }

    public long getSpaceLimitKb() {
        return spaceLimitKb;
    }

    public void setSpaceLimitKb(long spaceLimitKb) {
        this.spaceLimitKb = spaceLimitKb;
    }

    public int getLanguage() {
        return language;
    }

    public void setLanguage(int language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "JudgeRunRequest{" +
                "userCode='" + (userCode != null ? userCode.substring(0, Math.min(50, userCode.length())) + "..." : "null") + '\'' +
                ", mainMethod='" + mainMethod + '\'' +
                ", testInput='" + testInput + '\'' +
                ", expectedOutput='" + expectedOutput + '\'' +
                ", timeLimitMs=" + timeLimitMs +
                ", spaceLimitKb=" + spaceLimitKb +
                ", language=" + language +
                '}';
    }
}