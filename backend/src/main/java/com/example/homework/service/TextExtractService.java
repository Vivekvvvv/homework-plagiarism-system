package com.example.homework.service;

import com.example.homework.common.exception.BusinessException;
import com.example.homework.common.exception.ErrorCodes;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class TextExtractService {

    public String extractTextFromBytes(String fileName, String mimeType, byte[] content) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();

        if (lowerName.endsWith(".txt") || lowerMime.contains("text/plain")) {
            return new String(content, StandardCharsets.UTF_8);
        }

        if (lowerName.endsWith(".docx")
            || lowerMime.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractDocxText(content);
        }

        throw new BusinessException(ErrorCodes.BAD_REQUEST, "仅支持 txt 或 docx 文件");
    }

    private String extractDocxText(byte[] content) {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] xmlBytes = zis.readAllBytes();
                    String xml = new String(xmlBytes, StandardCharsets.UTF_8);
                    String text = xml
                        .replaceAll("<[^>]+>", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
                    return text;
                }
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST, "docx 解析失败");
        }
        throw new BusinessException(ErrorCodes.BAD_REQUEST, "docx 内容为空");
    }
}
