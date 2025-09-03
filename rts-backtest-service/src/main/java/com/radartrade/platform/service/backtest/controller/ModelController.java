package com.radartrade.platform.service.backtest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backtest/models")
public class ModelController {
    @Value("${app.model-storage-path}")
    private String storagePath;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadModel(@RequestParam("model") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File không được để trống.");
        }
        try {
            File storageDir = new File(storagePath);
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // Tạo tên file duy nhất để tránh trùng lặp
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            Path path = Paths.get(storagePath + File.separator + uniqueFilename);
            Files.copy(file.getInputStream(), path);

            return ResponseEntity.ok(uniqueFilename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Tải file lên thất bại: " + e.getMessage());
        }
    }
}