package com.example.timer_backend.exception.custom;

public class FileStorageException extends FileProcessingException {
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
