package com.example.timer_backend.config;

import com.example.timer_backend.converter.StringToFileTypeConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final StringToFileTypeConverter fileTypeConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(fileTypeConverter);
    }
}
