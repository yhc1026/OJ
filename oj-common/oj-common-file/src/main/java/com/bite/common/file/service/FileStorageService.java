package com.bite.common.file.service;

import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 文件存储统一接口。
 */
@Service
public interface FileStorageService {

    /**
     * 上传文件。
     *
     * @param stream      输入流
     * @param originalName 原始文件名（用于后缀识别）
     * @param contentType 内容类型（可为空）
     * @param bizDir      业务目录（可为空）
     * @return 访问 URL
     */
    String upload(InputStream stream, String originalName, String contentType, String bizDir);

    /**
     * 删除对象（object key）。
     */
    void delete(String objectKey);

    /**
     * 根据 object key 构建 URL。
     */
    String buildUrl(String objectKey);

    /**
     * 构建带时效签名的访问 URL（适用于私有读 Bucket）。
     *
     * @param objectKey     对象 key
     * @param expireSeconds 过期秒数
     */
    String buildSignedUrl(String objectKey, long expireSeconds);
}

