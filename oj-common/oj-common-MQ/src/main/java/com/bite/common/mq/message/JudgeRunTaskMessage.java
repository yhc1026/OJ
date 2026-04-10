package com.bite.common.mq.message;

import java.io.Serializable;

public class JudgeRunTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long submitId;
    private Long userId;
    private Long questionId;
    private Long examId;
    private String userCode;
    private String mainMethod;
    private String testInput;
    private String expectedOutput;
    private long timeLimitMs;
    private long spaceLimitKb;
    private int language;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getSubmitId() {
        return submitId;
    }

    public void setSubmitId(Long submitId) {
        this.submitId = submitId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
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
}
