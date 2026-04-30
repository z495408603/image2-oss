package com.image2oss.javaapp.model;

public class OssUploadRequest {
    private AliyunOssConfig oss;
    private String prompt;
    private Object index;
    private String image;

    public AliyunOssConfig getOss() {
        return oss;
    }

    public void setOss(AliyunOssConfig oss) {
        this.oss = oss;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Object getIndex() {
        return index;
    }

    public void setIndex(Object index) {
        this.index = index;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
