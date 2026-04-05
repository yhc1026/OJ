package com.bite.friend.domain.dto;

import lombok.Data;

/**
 * 请求头像上传 STS。
 */
@Data
public class UserAvatarStsRequest {
    /**
     * 可选目录片段，仅允许字母/数字/下划线/短横线/斜杠。
     */
    private String dir;
}

