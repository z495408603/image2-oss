package com.image2oss.javaapp.model;

public class AliyunOssConfig {
    private String provider;
    private String region;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String stsToken;
    private String endpoint;
    private String pathPrefix;
    private String publicBaseUrl;
    private Boolean secure;
    private Boolean useSignedUrl;
    private Integer signedUrlExpires;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getStsToken() {
        return stsToken;
    }

    public void setStsToken(String stsToken) {
        this.stsToken = stsToken;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public Boolean getSecure() {
        return secure;
    }

    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    public Boolean getUseSignedUrl() {
        return useSignedUrl;
    }

    public void setUseSignedUrl(Boolean useSignedUrl) {
        this.useSignedUrl = useSignedUrl;
    }

    public Integer getSignedUrlExpires() {
        return signedUrlExpires;
    }

    public void setSignedUrlExpires(Integer signedUrlExpires) {
        this.signedUrlExpires = signedUrlExpires;
    }
}
