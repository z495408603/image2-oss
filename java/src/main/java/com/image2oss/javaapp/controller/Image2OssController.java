package com.image2oss.javaapp.controller;

import com.image2oss.javaapp.model.AliyunOssConfig;
import com.image2oss.javaapp.model.OssUploadRequest;
import com.image2oss.javaapp.model.OssValidateRequest;
import com.image2oss.javaapp.service.OssUploadService;
import com.image2oss.javaapp.service.OssUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
public class Image2OssController {
    private final OssUploadService ossUploadService;

    public Image2OssController(OssUploadService ossUploadService) {
        this.ossUploadService = ossUploadService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("service", "image2-oss-java");
        response.put("provider", "aliyun");
        return response;
    }

    @GetMapping(value = {"/", "/gpt-image.html"})
    public ResponseEntity<?> index() {
        Path current = Paths.get("gpt-image.html").toAbsolutePath().normalize();
        Path parent = Paths.get("..", "gpt-image.html").toAbsolutePath().normalize();
        Path target = Files.exists(current) ? current : parent;
        if (Files.exists(target)) {
            Resource resource = new FileSystemResource(target.toFile());
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(resource);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body("未找到 gpt-image.html，请从仓库根目录访问页面文件。");
    }

    @PostMapping("/api/oss/validate")
    public Map<String, Object> validate(@RequestBody(required = false) OssValidateRequest request) {
        AliyunOssConfig config = OssUtils.normalizeAliyunConfig(request, System.getenv());
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("ok", true);
        response.put("provider", config.getProvider());
        response.put("bucket", config.getBucket());
        response.put("region", config.getRegion());
        response.put("pathPrefix", config.getPathPrefix());
        response.put("uploadMode", Boolean.TRUE.equals(config.getUseSignedUrl()) ? "private-signed-url" : "public-url");
        return response;
    }

    @PostMapping("/api/oss/upload")
    public Map<String, Object> upload(@RequestBody OssUploadRequest request) {
        AliyunOssConfig config = OssUtils.normalizeAliyunConfig(request.getOss(), System.getenv());
        return ossUploadService.upload(config, request.getPrompt(), request.getIndex(), request.getImage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException error) {
        return errorResponse(HttpStatus.BAD_REQUEST, error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAnyException(Exception error) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (message.contains("配置缺少") || message.contains("必须是") || message.contains("不支持") || message.contains("为空")) {
            status = HttpStatus.BAD_REQUEST;
        }
        return errorResponse(status, error);
    }

    private ResponseEntity<Map<String, String>> errorResponse(HttpStatus status, Exception error) {
        Map<String, String> response = new LinkedHashMap<String, String>();
        response.put("error", error.getMessage() == null ? "未知错误" : error.getMessage());
        return ResponseEntity.status(status)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(response);
    }
}
