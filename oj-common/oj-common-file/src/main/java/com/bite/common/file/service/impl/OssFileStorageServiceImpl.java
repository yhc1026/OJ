package com.bite.common.file.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.bite.common.file.config.OssProperties;
import com.bite.common.file.service.FileStorageService;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 阿里云 OSS 存储实现。
 */
public class OssFileStorageServiceImpl implements FileStorageService {

    private final OSS ossClient;
    private final OssProperties props;

    public OssFileStorageServiceImpl(OSS ossClient, OssProperties props) {
        this.ossClient = ossClient;
        this.props = props;
    }

    @Override
    public String upload(InputStream stream, String originalName, String contentType, String bizDir) {
        String key = buildObjectKey(originalName, bizDir);
        ObjectMetadata metadata = new ObjectMetadata();
        if (StringUtils.hasText(contentType)) {
            metadata.setContentType(contentType);
        }
        ossClient.putObject(props.getBucketName(), key, stream, metadata);
        return buildUrl(key);
    }

    @Override
    public void delete(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return;
        }
        ossClient.deleteObject(props.getBucketName(), objectKey.trim());
    }

    @Override
    public String buildUrl(String objectKey) {
        String key = objectKey == null ? "" : objectKey.trim();
        String cdn = trimSlash(props.getCdnDomain());
        if (StringUtils.hasText(cdn)) {
            return "https://" + cdn + "/" + key;
        }
        String endpoint = trimSlash(props.getEndpoint());
        return "https://" + props.getBucketName().trim() + "." + endpoint + "/" + key;
    }

    @Override
    public String buildSignedUrl(String objectKey, long expireSeconds) {
        if (!StringUtils.hasText(objectKey)) {
            return "";
        }
        long ttl = expireSeconds <= 0 ? 600 : expireSeconds;
        Date expiration = new Date(System.currentTimeMillis() + ttl * 1000L);
        URL signed = ossClient.generatePresignedUrl(props.getBucketName(), objectKey.trim(), expiration);
        return signed == null ? "" : signed.toString();
    }

    private String buildObjectKey(String originalName, String bizDir) {
        String base = normalizeDir(props.getBaseDir());
        String biz = normalizeDir(bizDir);
        LocalDate now = LocalDate.now();
        String datePath = now.getYear() + "/" + now.getMonthValue() + "/" + now.getDayOfMonth() + "/";
        String suffix = fileSuffix(originalName);
        return base + biz + datePath + UUID.randomUUID().toString().replace("-", "") + suffix;
    }

    private static String normalizeDir(String dir) {
        if (!StringUtils.hasText(dir)) {
            return "";
        }
        String s = dir.trim().replace("\\", "/");
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }

    private static String trimSlash(String v) {
        if (!StringUtils.hasText(v)) {
            return "";
        }
        String s = v.trim();
        while (s.startsWith("http://")) {
            s = s.substring("http://".length());
        }
        while (s.startsWith("https://")) {
            s = s.substring("https://".length());
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String fileSuffix(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        String n = name.trim();
        int idx = n.lastIndexOf('.');
        if (idx < 0 || idx == n.length() - 1) {
            return "";
        }
        String suf = n.substring(idx).toLowerCase(Locale.ROOT);
        return suf.length() > 10 ? "" : suf;
    }
}

