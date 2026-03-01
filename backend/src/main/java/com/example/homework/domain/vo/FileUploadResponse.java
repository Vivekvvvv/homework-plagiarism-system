package com.example.homework.domain.vo;

import lombok.Data;

@Data
public class FileUploadResponse {
    private Long fileId;
    private String fileName;
    private Long fileSize;
}
