package com.example.springbootjavarefresh.service;

import java.time.LocalDateTime;

public interface ObjectStorageService {
    void upload(String objectKey, byte[] payload, String contentType);

    SignedObjectUrl signGetUrl(String objectKey);

    record SignedObjectUrl(String url, LocalDateTime expiresAt) {}
}
