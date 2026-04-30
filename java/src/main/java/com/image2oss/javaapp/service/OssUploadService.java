package com.image2oss.javaapp.service;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.image2oss.javaapp.model.AliyunOssConfig;
import com.image2oss.javaapp.model.ParsedImage;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OssUploadService {

    public Map<String, Object> upload(AliyunOssConfig config, String prompt, Object index, String imageDataUrl) {
        ParsedImage image = OssUtils.parseDataUrlImage(imageDataUrl);
        String objectName = OssUtils.buildObjectName(prompt, index, image.getContentType(), config.getPathPrefix(), Instant.now());

        OSS client = createClient(config);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(image.getContentType());
            metadata.setCacheControl("public, max-age=31536000, immutable");
            client.putObject(config.getBucket(), objectName, new java.io.ByteArrayInputStream(image.getBytes()), metadata);

            String defaultUrl = OssUtils.buildDefaultOssUrl(config, objectName);
            String publicUrl = OssUtils.buildPublicUrl(config, objectName);
            String signedUrl = Boolean.TRUE.equals(config.getUseSignedUrl()) ? generateSignedUrl(client, config, objectName) : "";
            String url = signedUrl.isEmpty() ? publicUrl : signedUrl;

            Map<String, Object> response = new LinkedHashMap<String, Object>();
            response.put("ok", true);
            response.put("provider", config.getProvider());
            response.put("bucket", config.getBucket());
            response.put("objectName", objectName);
            response.put("contentType", image.getContentType());
            response.put("size", image.getBytes().length);
            response.put("defaultUrl", defaultUrl);
            response.put("publicUrl", publicUrl);
            response.put("signedUrl", signedUrl);
            response.put("url", url);
            return response;
        } finally {
            client.shutdown();
        }
    }

    private OSS createClient(AliyunOssConfig config) {
        String endpoint = buildSdkEndpoint(config);
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setProtocol(Boolean.TRUE.equals(config.getSecure()) ? com.aliyun.oss.common.comm.Protocol.HTTPS : com.aliyun.oss.common.comm.Protocol.HTTP);

        if (config.getStsToken() != null && !config.getStsToken().trim().isEmpty()) {
            return new OSSClientBuilder().build(endpoint, new DefaultCredentialProvider(
                    config.getAccessKeyId(),
                    config.getAccessKeySecret(),
                    config.getStsToken()
            ), configuration);
        }

        return new OSSClientBuilder().build(endpoint, config.getAccessKeyId(), config.getAccessKeySecret(), configuration);
    }

    private String generateSignedUrl(OSS client, AliyunOssConfig config, String objectName) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(config.getBucket(), objectName);
        request.setExpiration(new Date(System.currentTimeMillis() + config.getSignedUrlExpires().intValue() * 1000L));
        URL url = client.generatePresignedUrl(request);
        return url.toString();
    }

    private String buildSdkEndpoint(AliyunOssConfig config) {
        if (config.getEndpoint() != null && !config.getEndpoint().trim().isEmpty()) {
            String endpoint = config.getEndpoint().trim();
            if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
                return endpoint;
            }
            return (Boolean.TRUE.equals(config.getSecure()) ? "https://" : "http://") + endpoint;
        }
        return (Boolean.TRUE.equals(config.getSecure()) ? "https://" : "http://") + config.getRegion() + ".aliyuncs.com";
    }
}
