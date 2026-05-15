package com.example.timer_backend.provider;

import lombok.Getter;

@Getter
public enum FileType {
    CSV("csv", "text/csv");
    //  JSON("json", "application/json");

    private final String extension;
    private final String mimeType;

    FileType(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public static FileType fromString(String value) {
        return FileType.valueOf(value.toUpperCase());
    }
}
