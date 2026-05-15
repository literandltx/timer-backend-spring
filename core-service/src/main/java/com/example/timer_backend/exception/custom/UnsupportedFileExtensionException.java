package com.example.timer_backend.exception.custom;

public class UnsupportedFileExtensionException extends RuntimeException {
    public UnsupportedFileExtensionException(String message) {
        super(message);
    }
}
