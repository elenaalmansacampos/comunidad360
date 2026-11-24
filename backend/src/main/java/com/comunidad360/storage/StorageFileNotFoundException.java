package com.comunidad360.storage;

public class StorageFileNotFoundException extends RuntimeException {

    public StorageFileNotFoundException(String key) {
        super("Stored file not found: " + key);
    }
}
