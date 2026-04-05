package com.bite.common.file.service.impl;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.sts.model.v20150401.AssumeRoleRequest;
import com.aliyuncs.sts.model.v20150401.AssumeRoleResponse;
import com.bite.common.file.config.OssProperties;
import com.bite.common.file.service.OssStsService;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于阿里云 STS 的临时凭证签发实现。
 */
public class AliyunOssStsServiceImpl implements OssStsService {

    private final OssProperties props;

    public AliyunOssStsServiceImpl(OssProperties props) {
        this.props = props;
    }

    @Override
    public Map<String, String> issueForPrefix(String objectKeyPrefix) {
        String prefix = normalizePrefix(objectKeyPrefix);
        if (!StringUtils.hasText(prefix)) {
            throw new IllegalArgumentException("objectKeyPrefix 不能为空");
        }
        if (!StringUtils.hasText(props.getStsRoleArn())) {
            throw new IllegalStateException("缺少 oss.sts-role-arn 配置");
        }
        String region = StringUtils.hasText(props.getRegion()) ? props.getRegion().trim() : "oss-cn-hangzhou";
        DefaultProfile profile = DefaultProfile.getProfile(
                "",
                props.getAccessKeyId().trim(),
                props.getAccessKeySecret().trim()
        );
        DefaultAcsClient client = new DefaultAcsClient(profile);

        AssumeRoleRequest request = new AssumeRoleRequest();
        request.setSysEndpoint("sts.aliyuncs.com");
        request.setRoleArn(props.getStsRoleArn().trim());
        request.setRoleSessionName(StringUtils.hasText(props.getStsRoleSessionName())
                ? props.getStsRoleSessionName().trim()
                : "oj-friend-avatar");
        request.setDurationSeconds(Math.max(900L, props.getStsDurationSeconds() == null ? 900L : props.getStsDurationSeconds().longValue()));
        request.setPolicy(buildPolicy(prefix));

        try {
            AssumeRoleResponse response = client.getAcsResponse(request);
            Map<String, String> out = new LinkedHashMap<>();
            out.put("accessKeyId", response.getCredentials().getAccessKeyId());
            out.put("accessKeySecret", response.getCredentials().getAccessKeySecret());
            out.put("securityToken", response.getCredentials().getSecurityToken());
            out.put("expiration", response.getCredentials().getExpiration());
            out.put("bucketName", props.getBucketName());
            out.put("endpoint", props.getEndpoint());
            out.put("region", region);
            out.put("objectKeyPrefix", prefix);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("申请 OSS STS 失败: " + e.getMessage(), e);
        }
    }

    private String buildPolicy(String prefix) {
        String resource = "acs:oss:*:*:" + props.getBucketName().trim() + "/" + prefix + "*";
        return "{\n" +
                "  \"Version\": \"1\",\n" +
                "  \"Statement\": [\n" +
                "    {\n" +
                "      \"Effect\": \"Allow\",\n" +
                "      \"Action\": [\"oss:PutObject\", \"oss:AbortMultipartUpload\", \"oss:ListParts\"],\n" +
                "      \"Resource\": [\"" + resource + "\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private static String normalizePrefix(String in) {
        if (!StringUtils.hasText(in)) {
            return "";
        }
        String s = in.trim().replace("\\", "/");
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        if (!s.endsWith("/")) {
            s = s + "/";
        }
        return s;
    }
}

