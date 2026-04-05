package com.bite.common.file.service.impl;

import com.bite.common.file.service.OssStsService;

import java.util.Map;

/**
 * OSS 未启用时的 STS 兜底实现。
 */
public class DisabledOssStsServiceImpl implements OssStsService {
    @Override
    public Map<String, String> issueForPrefix(String objectKeyPrefix) {
        throw new IllegalStateException("OSS 未启用或 STS 未配置，无法签发临时凭证");
    }
}

