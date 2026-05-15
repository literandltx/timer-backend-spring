package com.example.timer_backend.converter;

import com.example.timer_backend.provider.FileType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToFileTypeConverter implements Converter<String, FileType> {
    @Override
    public FileType convert(String source) {
        return FileType.valueOf(source.trim().toUpperCase());
    }
}
