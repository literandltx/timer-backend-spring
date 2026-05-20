package com.literandltx.reportservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class S3UploadService {
    private static final String localStoragePath = "./storage/";

    public void uploadFile(String s3Key, byte[] fileBytes) {
        try {
            Path targetPath = Paths.get(localStoragePath, s3Key);

            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileBytes);

            log.info("Saved file to: {}", targetPath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save file to local disk", e);
            throw new RuntimeException("Storage failure", e);
        }
    }
}
