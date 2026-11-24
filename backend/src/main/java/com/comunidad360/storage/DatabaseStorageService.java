package com.comunidad360.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "database")
public class DatabaseStorageService implements StorageService {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseStorageService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String store(byte[] content, String extension, String contentType) {
        String key = UUID.randomUUID() + "." + extension;
        jdbcTemplate.update("""
                insert into stored_file (storage_key, content, content_type)
                values (?, ?, ?)
                """, key, content, contentType);
        return key;
    }

    @Override
    public byte[] read(String key) {
        validateKey(key);
        try {
            return jdbcTemplate.queryForObject(
                    "select content from stored_file where storage_key = ?", byte[].class, key);
        } catch (EmptyResultDataAccessException exception) {
            throw new StorageFileNotFoundException(key);
        }
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        jdbcTemplate.update("delete from stored_file where storage_key = ?", key);
    }

    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        return null;
    }

    @Override
    public boolean supportsPresignedUrls() {
        return false;
    }

    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                    "select exists(select 1 from stored_file where storage_key = ?)", Boolean.class, key);
            return Boolean.TRUE.equals(exists);
        } catch (DataAccessException exception) {
            return true;
        }
    }

    private void validateKey(String key) {
        if (key == null || !key.matches("[a-f0-9-]+\\.[a-z0-9]+")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
    }
}
