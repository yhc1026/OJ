package com.bite.common.file.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.bite.common.file.service.FileStorageService;
import com.bite.common.file.service.OssStsService;
import com.bite.common.file.service.impl.AliyunOssStsServiceImpl;
import com.bite.common.file.service.impl.DisabledFileStorageServiceImpl;
import com.bite.common.file.service.impl.DisabledOssStsServiceImpl;
import com.bite.common.file.service.impl.OssFileStorageServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * OSS 自动装配。
 */
@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true")
    public OSS ossClient(OssProperties props) {
        if (!StringUtils.hasText(props.getEndpoint())
                || !StringUtils.hasText(props.getAccessKeyId())
                || !StringUtils.hasText(props.getAccessKeySecret())) {
            throw new IllegalStateException("OSS 已启用，但 endpoint/accessKeyId/accessKeySecret 未完整配置");
        }
        return new OSSClientBuilder().build(
                props.getEndpoint().trim(),
                props.getAccessKeyId().trim(),
                props.getAccessKeySecret().trim()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true")
    public FileStorageService fileStorageService(OSS ossClient, OssProperties props) {
        return new OssFileStorageServiceImpl(ossClient, props);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "oss.enabled", havingValue = "true")
    public OssStsService ossStsService(OssProperties props) {
        return new AliyunOssStsServiceImpl(props);
    }

    @Bean
    @ConditionalOnMissingBean(FileStorageService.class)
    public FileStorageService disabledFileStorageService() {
        return new DisabledFileStorageServiceImpl();
    }

    @Bean
    @ConditionalOnMissingBean(OssStsService.class)
    public OssStsService disabledOssStsService() {
        return new DisabledOssStsServiceImpl();
    }
}

