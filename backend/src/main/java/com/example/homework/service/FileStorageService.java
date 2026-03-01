package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import com.example.homework.domain.entity.FileStorage;
import com.example.homework.mapper.FileStorageMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileStorageService {

    private final FileStorageMapper fileStorageMapper;
    private final Path uploadRoot;
    private final long maxUploadBytes;

    public FileStorageService(FileStorageMapper fileStorageMapper,
                              @Value("${app.storage.upload-dir:uploads}") String uploadDir,
                              @Value("${spring.servlet.multipart.max-file-size:10MB}") DataSize maxFileSize) {
        this.fileStorageMapper = fileStorageMapper;
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.maxUploadBytes = maxFileSize.toBytes();
    }

    public FileStorage save(MultipartFile file, Long uploadedBy) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "File cannot be empty");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "File too large");
        }

        try {
            Files.createDirectories(uploadRoot);
            String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
            String storeName = UUID.randomUUID() + "_" + originalName.replace(" ", "_");
            Path target = uploadRoot.resolve(storeName);
            file.transferTo(target);

            FileStorage record = new FileStorage();
            record.setFileName(originalName);
            record.setFilePath(target.toString());
            record.setFileSize(file.getSize());
            record.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            record.setUploadedBy(uploadedBy);
            record.setUploadedAt(LocalDateTime.now());
            fileStorageMapper.insert(record);
            return record;
        } catch (IOException e) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Failed to store file");
        }
    }

    public FileStorage getById(Long fileId) {
        FileStorage fileStorage = fileStorageMapper.selectOne(new LambdaQueryWrapper<FileStorage>()
            .eq(FileStorage::getId, fileId)
            .last("LIMIT 1"));
        if (fileStorage == null) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "File not found");
        }
        return fileStorage;
    }

    public byte[] readFileBytes(FileStorage fileStorage) {
        try {
            return Files.readAllBytes(Path.of(fileStorage.getFilePath()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR, "Failed to read file");
        }
    }
}
