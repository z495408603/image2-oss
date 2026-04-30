package com.image2oss.javaapp.service;

import com.image2oss.javaapp.model.AliyunOssConfig;
import com.image2oss.javaapp.model.ParsedImage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OssUtilsTest {

    @Test
    void normalizeAliyunConfigReadsExplicitConfigAndValidatesRequiredFields() {
        AliyunOssConfig input = new AliyunOssConfig();
        input.setRegion("oss-cn-hangzhou");
        input.setBucket("demo-bucket");
        input.setAccessKeyId("id");
        input.setAccessKeySecret("secret");
        input.setPathPrefix(" image/output ");
        input.setUseSignedUrl(true);

        AliyunOssConfig config = OssUtils.normalizeAliyunConfig(input, Collections.<String, String>emptyMap());

        assertEquals("aliyun", config.getProvider());
        assertEquals("oss-cn-hangzhou", config.getRegion());
        assertEquals("demo-bucket", config.getBucket());
        assertEquals("image/output", config.getPathPrefix());
        assertTrue(Boolean.TRUE.equals(config.getUseSignedUrl()));
    }

    @Test
    void normalizeAliyunConfigReportsMissingFields() {
        AliyunOssConfig input = new AliyunOssConfig();
        input.setBucket("demo");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> OssUtils.normalizeAliyunConfig(input, Collections.<String, String>emptyMap()));

        assertTrue(error.getMessage().contains("region, accessKeyId, accessKeySecret"));
    }

    @Test
    void parseDataUrlImageParsesSupportedImageDataUrls() {
        ParsedImage parsed = OssUtils.parseDataUrlImage("data:image/png;base64,aGVsbG8=");
        assertEquals("image/png", parsed.getContentType());
        assertEquals("png", parsed.getExtension());
        assertEquals("hello", new String(parsed.getBytes()));
    }

    @Test
    void buildObjectNameUsesPrefixDateFoldersPromptSlugAndExtension() {
        String objectName = OssUtils.buildObjectName(
                "赛博 城市 / Cyber City",
                1,
                "image/jpeg",
                "/ai//images/",
                Instant.parse("2026-04-29T08:00:00Z")
        );

        assertTrue(objectName.matches("^ai/images/2026/04/29/20260429080000-2-赛博-城市-cyber-city-[a-f0-9]{8}\\.jpg$"));
    }

    @Test
    void buildPublicUrlUsesCustomPublicBaseUrlWhenProvided() {
        AliyunOssConfig config = new AliyunOssConfig();
        config.setPublicBaseUrl("https://cdn.example.com/assets/");
        config.setBucket("bucket");
        config.setRegion("oss-cn-hangzhou");
        config.setSecure(true);

        String url = OssUtils.buildPublicUrl(config, "ai/a.png");
        assertEquals("https://cdn.example.com/assets/ai/a.png", url);
    }

    @Test
    void buildDefaultOssUrlUsesAliyunBucketEndpointWhenNoCustomDomainIsNeeded() {
        AliyunOssConfig config = new AliyunOssConfig();
        config.setBucket("gpt-image2-shadow");
        config.setRegion("oss-cn-beijing");
        config.setSecure(false);

        String url = OssUtils.buildDefaultOssUrl(config, "ai-images/a.png");
        assertEquals("http://gpt-image2-shadow.oss-cn-beijing.aliyuncs.com/ai-images/a.png", url);
    }
}
