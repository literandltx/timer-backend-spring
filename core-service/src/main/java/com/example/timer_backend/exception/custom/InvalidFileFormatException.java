package com.example.timer_backend.exception.custom;

public class InvalidFileFormatException extends FileProcessingException {
    public InvalidFileFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

