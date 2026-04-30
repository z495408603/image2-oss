package com.image2oss.javaapp.model;

public class OssValidateRequest {
    private AliyunOssConfig oss;
    private String provider;
    private String region;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String stsToken;
    private String endpoint;
    private String pathPrefix;
    private String publicBaseUrl;
    private Object secure;
    private Object useSignedUrl;
    private Object signedUrlExpires;

    public AliyunOssConfig getOss() {
        return oss;
    }

    public void setOss(AliyunOssConfig oss) {
        this.oss = oss;
    }

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

    public Object getSecure() {
        return secure;
    }

    public void setSecure(Object secure) {
        this.secure = secure;
    }

    public Object getUseSignedUrl() {
        return useSignedUrl;
    }

    public void setUseSignedUrl(Object useSignedUrl) {
        this.useSignedUrl = useSignedUrl;
    }

    public Object getSignedUrlExpires() {
        return signedUrlExpires;
    }

    public void setSignedUrlExpires(Object signedUrlExpires) {
        this.signedUrlExpires = signedUrlExpires;
    }
}
