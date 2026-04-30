package com.image2oss.javaapp.service;

import com.image2oss.javaapp.model.AliyunOssConfig;
import com.image2oss.javaapp.model.OssValidateRequest;
import com.image2oss.javaapp.model.ParsedImage;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class OssUtils {
    private static final Map<String, String> SUPPORTED_CONTENT_TYPES = new LinkedHashMap<String, String>();
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("^data:(image/(?:png|jpeg|webp));base64,([a-z0-9+/=\\r\\n]+)$", Pattern.CASE_INSENSITIVE);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    static {
        SUPPORTED_CONTENT_TYPES.put("image/png", "png");
        SUPPORTED_CONTENT_TYPES.put("image/jpeg", "jpg");
        SUPPORTED_CONTENT_TYPES.put("image/webp", "webp");
    }

    private OssUtils() {
    }

    public static AliyunOssConfig normalizeAliyunConfig(AliyunOssConfig input, Map<String, String> env) {
        AliyunOssConfig raw = input == null ? new AliyunOssConfig() : input;
        AliyunOssConfig config = new AliyunOssConfig();
        config.setProvider(cleanString(firstNonEmpty(raw.getProvider(), env.get("OSS_PROVIDER"), "aliyun")).toLowerCase(Locale.ROOT));
        config.setRegion(cleanString(firstNonEmpty(raw.getRegion(), env.get("OSS_REGION"))));
        config.setBucket(cleanString(firstNonEmpty(raw.getBucket(), env.get("OSS_BUCKET"))));
        config.setAccessKeyId(cleanString(firstNonEmpty(raw.getAccessKeyId(), env.get("OSS_ACCESS_KEY_ID"))));
        config.setAccessKeySecret(cleanString(firstNonEmpty(raw.getAccessKeySecret(), env.get("OSS_ACCESS_KEY_SECRET"))));
        config.setStsToken(cleanString(firstNonEmpty(raw.getStsToken(), env.get("OSS_STS_TOKEN"))));
        config.setEndpoint(cleanString(firstNonEmpty(raw.getEndpoint(), env.get("OSS_ENDPOINT"))));
        config.setPathPrefix(cleanString(firstNonEmpty(raw.getPathPrefix(), env.get("OSS_PATH_PREFIX"), "ai-images")));
        config.setPublicBaseUrl(cleanString(firstNonEmpty(raw.getPublicBaseUrl(), env.get("OSS_PUBLIC_BASE_URL"))));
        config.setSecure(Boolean.valueOf(toBoolean(raw.getSecure(), toBoolean(env.get("OSS_SECURE"), true))));
        config.setUseSignedUrl(Boolean.valueOf(toBoolean(raw.getUseSignedUrl(), toBoolean(env.get("OSS_USE_SIGNED_URL"), false))));
        config.setSignedUrlExpires(Integer.valueOf(toInt(raw.getSignedUrlExpires(), toInt(env.get("OSS_SIGNED_URL_EXPIRES"), 3600))));

        if (!"aliyun".equals(config.getProvider())) {
            throw new IllegalArgumentException("当前后端仅内置阿里云 OSS provider");
        }

        List<String> missing = new ArrayList<String>();
        if (config.getRegion().isEmpty()) {
            missing.add("region");
        }
        if (config.getBucket().isEmpty()) {
            missing.add("bucket");
        }
        if (config.getAccessKeyId().isEmpty()) {
            missing.add("accessKeyId");
        }
        if (config.getAccessKeySecret().isEmpty()) {
            missing.add("accessKeySecret");
        }
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("OSS 配置缺少字段：" + String.join(", ", missing));
        }

        if (config.getSignedUrlExpires().intValue() <= 0) {
            config.setSignedUrlExpires(Integer.valueOf(3600));
        }

        return config;
    }

    public static AliyunOssConfig normalizeAliyunConfig(OssValidateRequest request, Map<String, String> env) {
        if (request == null) {
            return normalizeAliyunConfig((AliyunOssConfig) null, env);
        }
        if (request.getOss() != null) {
            return normalizeAliyunConfig(request.getOss(), env);
        }

        AliyunOssConfig input = new AliyunOssConfig();
        input.setProvider(request.getProvider());
        input.setRegion(request.getRegion());
        input.setBucket(request.getBucket());
        input.setAccessKeyId(request.getAccessKeyId());
        input.setAccessKeySecret(request.getAccessKeySecret());
        input.setStsToken(request.getStsToken());
        input.setEndpoint(request.getEndpoint());
        input.setPathPrefix(request.getPathPrefix());
        input.setPublicBaseUrl(request.getPublicBaseUrl());
        input.setSecure(toBoolean(request.getSecure(), true));
        input.setUseSignedUrl(toBoolean(request.getUseSignedUrl(), false));
        input.setSignedUrlExpires(toInt(request.getSignedUrlExpires(), 3600));
        return normalizeAliyunConfig(input, env);
    }

    public static ParsedImage parseDataUrlImage(String input) {
        String value = cleanString(input);
        java.util.regex.Matcher matcher = DATA_URL_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("图片数据必须是 PNG、JPG 或 WEBP 的 Data URL");
        }

        String contentType = matcher.group(1).toLowerCase(Locale.ROOT);
        String base64 = matcher.group(2).replaceAll("\\s", "");
        byte[] bytes = Base64.getDecoder().decode(base64.getBytes(StandardCharsets.UTF_8));
        if (bytes.length == 0) {
            throw new IllegalArgumentException("图片数据为空");
        }

        return new ParsedImage(bytes, contentType, getExtension(contentType));
    }

    public static String buildObjectName(String prompt, Object index, String contentType, String pathPrefix, Instant now) {
        String extension = getExtension(contentType);
        ZonedDateTime dateTime = ZonedDateTime.ofInstant(now, ZoneOffset.UTC);
        String yyyy = String.format(Locale.ROOT, "%04d", dateTime.getYear());
        String mm = String.format(Locale.ROOT, "%02d", dateTime.getMonthValue());
        String dd = String.format(Locale.ROOT, "%02d", dateTime.getDayOfMonth());
        String stamp = STAMP_FORMAT.format(dateTime);
        String random = randomHex(4);
        int safeIndex = normalizeIndex(index);
        return joinPrefix(pathPrefix) + yyyy + "/" + mm + "/" + dd + "/" + stamp + "-" + safeIndex + "-" + slugifyPrompt(prompt) + "-" + random + "." + extension;
    }

    public static String buildDefaultOssUrl(AliyunOssConfig config, String objectName) {
        if (!cleanString(config.getEndpoint()).isEmpty()) {
            String endpoint = config.getEndpoint().replaceFirst("^https?://", "").replaceAll("/+$", "");
            return (Boolean.TRUE.equals(config.getSecure()) ? "https" : "http") + "://" + config.getBucket() + "." + endpoint + "/" + objectName;
        }
        return (Boolean.TRUE.equals(config.getSecure()) ? "https" : "http") + "://" + config.getBucket() + "." + config.getRegion() + ".aliyuncs.com/" + objectName;
    }

    public static String buildPublicUrl(AliyunOssConfig config, String objectName) {
        if (!cleanString(config.getPublicBaseUrl()).isEmpty()) {
            return config.getPublicBaseUrl().replaceAll("/+$", "") + "/" + objectName;
        }
        return buildDefaultOssUrl(config, objectName);
    }

    private static String cleanString(String value) {
        return value == null ? "" : value.trim();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private static boolean toBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            String normalized = ((String) value).trim().toLowerCase(Locale.ROOT);
            if ("true".equals(normalized) || "1".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized)) {
                return true;
            }
            if ("false".equals(normalized) || "0".equals(normalized) || "no".equals(normalized) || "off".equals(normalized)) {
                return false;
            }
        }
        return fallback;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String joinPrefix(String prefix) {
        String cleaned = cleanString(prefix).replace("\\", "/").replaceAll("^/+|/+$", "").replaceAll("/{2,}", "/");
        return cleaned.isEmpty() ? "" : cleaned + "/";
    }

    private static String slugifyPrompt(String prompt) {
        String slug = cleanString(prompt)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > 48) {
            slug = slug.substring(0, 48);
        }
        return slug.isEmpty() ? "ai-image" : slug;
    }

    private static String getExtension(String contentType) {
        String extension = SUPPORTED_CONTENT_TYPES.get(contentType);
        if (extension == null) {
            throw new IllegalArgumentException("不支持的图片类型：" + (contentType == null || contentType.isEmpty() ? "unknown" : contentType));
        }
        return extension;
    }

    private static int normalizeIndex(Object index) {
        try {
            int value = Integer.parseInt(String.valueOf(index));
            return Math.max(0, value) + 1;
        } catch (Exception ignored) {
            return 1;
        }
    }

    private static String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        RANDOM.nextBytes(buffer);
        StringBuilder builder = new StringBuilder(bytes * 2);
        for (byte value : buffer) {
            builder.append(String.format(Locale.ROOT, "%02x", value));
        }
        return builder.toString();
    }
}
