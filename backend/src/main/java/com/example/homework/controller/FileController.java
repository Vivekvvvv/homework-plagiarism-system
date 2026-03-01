package com.example.homework.controller;

import com.example.homework.common.ApiResponse;
import com.example.homework.common.audit.AuditAction;
import com.example.homework.domain.entity.FileStorage;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.vo.FileUploadResponse;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.example.homework.service.FileStorageService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static com.example.homework.controller.ControllerSupport.currentUser;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final AuthService authService;
    private final AuditLogService auditLogService;

    public FileController(FileStorageService fileStorageService,
                          AuthService authService,
                          AuditLogService auditLogService) {
        this.fileStorageService = fileStorageService;
        this.authService = authService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/upload")
    public ApiResponse<FileUploadResponse> upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        SysUser current = currentUser(authService, authentication);
        FileStorage saved = fileStorageService.save(file, current.getId());

        FileUploadResponse response = new FileUploadResponse();
        response.setFileId(saved.getId());
        response.setFileName(saved.getFileName());
        response.setFileSize(saved.getFileSize());

        auditLogService.log(current, AuditAction.FILE_UPLOAD.name(), "file_storage",
            String.valueOf(saved.getId()), saved.getFileName(), "/api/v1/files/upload", "POST");
        return ApiResponse.ok(response);
    }

}
