package com.bite.judge.domain.dto.friend;

import java.io.Serializable;

/**
 * 单用例判题结果（judge → friend，经 OpenFeign）。
 * <p>
 * 独立于内部逻辑，确保 Feign 序列化时结构稳定。
 */
public class JudgeSingleCaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int verdict;
    private String actualOutput;
    private String compileLog;
    private String stderr;
    private String message;

    public JudgeSingleCaseResponse() {
    }

    public int getVerdict() {
        return verdict;
    }

    public void setVerdict(int verdict) {
        this.verdict = verdict;
    }

    public String getActualOutput() {
        return actualOutput;
    }

    public void setActualOutput(String actualOutput) {
        this.actualOutput = actualOutput;
    }

    public String getCompileLog() {
        return compileLog;
    }

    public void setCompileLog(String compileLog) {
        this.compileLog = compileLog;
    }

    public String getStderr() {
        return stderr;
    }

    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "JudgeSingleCaseResponse{" +
                "verdict=" + verdict +
                ", actualOutput='" + actualOutput + '\'' +
                ", compileLog='" + compileLog + '\'' +
                ", stderr='" + stderr + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}