package com.example.timer_backend.provider;

import com.example.timer_backend.exception.custom.UnsupportedFileExtensionException;
import com.example.timer_backend.service.FileHandler;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FileHandlerProvider {
    private final Map<FileType, FileHandler> handlerMap;

    public FileHandlerProvider(List<FileHandler> handlers) {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(
                        FileHandler::getSupportedType,
                        handler -> handler
                ));
    }

    public FileHandler getHandler(FileType type) {
        FileHandler handler = handlerMap.get(type);

        if (handler == null) {
            throw new UnsupportedFileExtensionException("No handler registered for: " + type);
        }
        return handler;
    }
}
