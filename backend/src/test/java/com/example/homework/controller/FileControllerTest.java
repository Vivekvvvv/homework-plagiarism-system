package com.example.homework.controller;

import com.example.homework.common.exception.GlobalExceptionHandler;
import com.example.homework.domain.entity.FileStorage;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.security.UserRole;
import com.example.homework.service.AuditLogService;
import com.example.homework.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private com.example.homework.service.FileStorageService fileStorageService;

    @Mock
    private AuthService authService;

    @Mock
    private AuditLogService auditLogService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FileController(fileStorageService, authService, auditLogService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(
                new MappingJackson2HttpMessageConverter(objectMapper),
                new ResourceHttpMessageConverter()
            )
            .setValidator(validator)
            .build();
    }

    @Test
    void uploadShouldReturnFileUploadResponse() throws Exception {
        SysUser teacher = teacherUser();
        FileStorage saved = new FileStorage();
        saved.setId(10L);
        saved.setFileName("test.txt");
        saved.setFileSize(5L);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(fileStorageService.save(any(), eq(2L))).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/v1/files/upload").file(file).principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.fileId").value(10))
            .andExpect(jsonPath("$.data.fileName").value("test.txt"))
            .andExpect(jsonPath("$.data.fileSize").value(5));

        verify(fileStorageService).save(any(), eq(2L));
    }

    @Test
    void uploadShouldLogAuditAfterSuccess() throws Exception {
        SysUser teacher = teacherUser();
        FileStorage saved = new FileStorage();
        saved.setId(11L);
        saved.setFileName("report.pdf");
        saved.setFileSize(2048L);

        when(authService.getCurrentUser(any(Authentication.class))).thenReturn(teacher);
        when(fileStorageService.save(any(), eq(2L))).thenReturn(saved);

        MockMultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", new byte[2048]);

        mockMvc.perform(multipart("/api/v1/files/upload").file(file).principal(authentication()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.fileId").value(11))
            .andExpect(jsonPath("$.data.fileName").value("report.pdf"))
            .andExpect(jsonPath("$.data.fileSize").value(2048));

        verify(auditLogService).log(eq(teacher), eq("FILE_UPLOAD"), eq("file_storage"),
            eq("11"), eq("report.pdf"), eq("/api/v1/files/upload"), eq("POST"));
    }

    private static SysUser teacherUser() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("teacher1");
        user.setRole(UserRole.TEACHER);
        return user;
    }

    private static Authentication authentication() {
        return new TestingAuthenticationToken("teacher1", null);
    }
}
