package com.bite.common.elasticsearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * 启用 ES Repository 扫描（公共组件）。
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "com.bite")
public class ElasticsearchRepositoryConfig {
}

