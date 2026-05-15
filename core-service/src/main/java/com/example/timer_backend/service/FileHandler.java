package com.example.timer_backend.service;

import com.example.timer_backend.model.TimerEntry;
import com.example.timer_backend.provider.FileType;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public interface FileHandler {
    List<TimerEntry> importFile(MultipartFile file);

    byte[] exportFile(List<TimerEntry> list);

    FileType getSupportedType();
}
