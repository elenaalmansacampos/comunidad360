package com.comunidad360.storage;

import com.comunidad360.config.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(AppProperties properties) {
        this.root = Path.of(properties.getStorage().getLocalPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create storage directory", exception);
        }
    }

    @Override
    public String store(byte[] content, String extension, String contentType) {
        String key = UUID.randomUUID() + "." + extension;
        try {
            Files.write(resolve(key), content, StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot store file", exception);
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read file", exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot delete file", exception);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        return null;
    }

    @Override
    public boolean supportsPresignedUrls() {
        return false;
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9-]+\\.[a-z0-9]+")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
