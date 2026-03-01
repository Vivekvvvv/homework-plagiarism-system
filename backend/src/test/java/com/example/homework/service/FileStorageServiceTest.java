package com.example.homework.service;

import com.example.homework.common.exception.BusinessException;
import com.example.homework.mapper.FileStorageMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileStorageMapper fileStorageMapper;

    @TempDir
    Path tempDir;

    @Test
    void saveShouldRejectEmptyFile() {
        FileStorageService service = new FileStorageService(fileStorageMapper, tempDir.toString(), DataSize.ofKilobytes(1));
        MockMultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(file, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void saveShouldRejectFileExceedingMaxSize() {
        FileStorageService service = new FileStorageService(fileStorageMapper, tempDir.toString(), DataSize.ofBytes(10));
        MockMultipartFile file = new MockMultipartFile("file", "large.txt", "text/plain", new byte[11]);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.save(file, 1L));
        assertEquals(400, ex.getCode());
    }
}
