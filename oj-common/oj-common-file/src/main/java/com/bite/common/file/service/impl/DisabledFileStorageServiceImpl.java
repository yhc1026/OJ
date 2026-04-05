package com.bite.common.file.service.impl;

import com.bite.common.file.service.FileStorageService;

import java.io.InputStream;

/**
 * OSS 未启用时的兜底实现，避免业务服务启动失败。
 */
public class DisabledFileStorageServiceImpl implements FileStorageService {
    @Override
    public String upload(InputStream stream, String originalName, String contentType, String bizDir) {
        throw new IllegalStateException("OSS 未启用，无法上传文件");
    }

    @Override
    public void delete(String objectKey) {
        // no-op
    }

    @Override
    public String buildUrl(String objectKey) {
        return "";
    }

    @Override
    public String buildSignedUrl(String objectKey, long expireSeconds) {
        return "";
    }
}

