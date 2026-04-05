package com.bite.common.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS 配置项（对应 common-oss.yaml）。
 */
@ConfigurationProperties(prefix = "oss")
public class OssProperties {
    private boolean enabled = false;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String baseDir = "oj/";
    private String cdnDomain;
    private String region = "oss-cn-hangzhou";
    private String stsRoleArn;
    private String stsRoleSessionName = "oj-friend-avatar";
    private Integer stsDurationSeconds = 900;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public String getCdnDomain() {
        return cdnDomain;
    }

    public void setCdnDomain(String cdnDomain) {
        this.cdnDomain = cdnDomain;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getStsRoleArn() {
        return stsRoleArn;
    }

    public void setStsRoleArn(String stsRoleArn) {
        this.stsRoleArn = stsRoleArn;
    }

    public String getStsRoleSessionName() {
        return stsRoleSessionName;
    }

    public void setStsRoleSessionName(String stsRoleSessionName) {
        this.stsRoleSessionName = stsRoleSessionName;
    }

    public Integer getStsDurationSeconds() {
        return stsDurationSeconds;
    }

    public void setStsDurationSeconds(Integer stsDurationSeconds) {
        this.stsDurationSeconds = stsDurationSeconds;
    }
}

