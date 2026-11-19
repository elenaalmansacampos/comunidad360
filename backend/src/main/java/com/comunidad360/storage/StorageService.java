package com.comunidad360.storage;

import java.time.Duration;

public interface StorageService {

    String store(byte[] content, String extension, String contentType);

    default String store(byte[] content, String extension) {
        return store(content, extension, "application/octet-stream");
    }

    byte[] read(String key);

    void delete(String key);

    String presignedGetUrl(String key, Duration ttl);

    boolean supportsPresignedUrls();
}
