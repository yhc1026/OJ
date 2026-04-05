package com.bite.friend.controller;

import com.bite.common.file.service.FileStorageService;
import com.bite.domain.Result;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户侧文件上传（示例：头像/附件）。
 */
@RestController
@RequestMapping("/friend/file")
public class UserFileController {

    private final FileStorageService fileStorageService;

    public UserFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传文件到 OSS，返回 url。
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dir", required = false) String dir) throws IOException {
        if (file == null || file.isEmpty()) {
            return Result.fail("file 不能为空");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            return Result.fail("文件大小不能超过 10MB");
        }
        String name = file.getOriginalFilename();
        if (!StringUtils.hasText(name)) {
            name = "upload.bin";
        }
        String url;
        try (var in = file.getInputStream()) {
            url = fileStorageService.upload(in, name, file.getContentType(), dir);
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", url);
        data.put("fileName", name);
        return Result.ok("success", data);
    }
}

