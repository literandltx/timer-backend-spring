package com.example.timer_backend.dto.export;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.core.io.Resource;

@Data
@Builder
@AllArgsConstructor
public class ExportResource {
    private final Resource resource;
    private final String fileName;
    private final String contentType;
}
