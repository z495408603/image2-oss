package com.image2oss.javaapp.model;

public class ParsedImage {
    private final byte[] bytes;
    private final String contentType;
    private final String extension;

    public ParsedImage(byte[] bytes, String contentType, String extension) {
        this.bytes = bytes;
        this.contentType = contentType;
        this.extension = extension;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }
}
