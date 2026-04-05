package com.bite.common.file.service;

import java.util.Map;

/**
 * OSS STS 临时凭证服务。
 */
public interface OssStsService {

    /**
     * 按 object key 前缀签发只读写该目录的 STS 凭证。
     *
     * @param objectKeyPrefix 允许上传的前缀（例如 oj/avatar/123/）
     * @return STS 参数（accessKeyId/accessKeySecret/securityToken 等）
     */
    Map<String, String> issueForPrefix(String objectKeyPrefix);
}

