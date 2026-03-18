package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.domain.entity.AuditLog;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.mapper.AuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogMapper auditLogMapper;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogMapper);
    }

    // --- log ---

    @Test
    void logShouldInsertAuditLog() {
        SysUser actor = new SysUser();
        actor.setUsername("admin");
        actor.setRole("ADMIN");
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        auditLogService.log(actor, "COURSE_CREATE", "course", "1", "CS101", "/api/v1/courses", "POST");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        AuditLog captured = captor.getValue();
        assertEquals("admin", captured.getActorUsername());
        assertEquals("ADMIN", captured.getActorRole());
        assertEquals("COURSE_CREATE", captured.getAction());
        assertEquals("course", captured.getTargetType());
        assertEquals("1", captured.getTargetId());
        assertNotNull(captured.getCreatedAt());
    }

    @Test
    void logShouldUseAnonymousWhenActorNull() {
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        auditLogService.log(null, "TEST", "test", "1", "detail", "/test", "GET");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals("anonymous", captor.getValue().getActorUsername());
        assertNull(captor.getValue().getActorRole());
    }

    @Test
    void logShouldTruncateLongDetail() {
        SysUser actor = new SysUser();
        actor.setUsername("admin");
        when(auditLogMapper.insert(any(AuditLog.class))).thenReturn(1);

        String longDetail = "x".repeat(2000);
        auditLogService.log(actor, "TEST", "test", "1", longDetail, "/test", "GET");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogMapper).insert(captor.capture());
        assertEquals(1000, captor.getValue().getDetail().length());
    }

    // --- list ---

    @Test
    @SuppressWarnings("unchecked")
    void listShouldFilterByActorUsername() {
        AuditLog log1 = new AuditLog();
        log1.setId(1L);
        when(auditLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(log1));

        List<AuditLog> result = auditLogService.list("admin", null, 10);

        assertEquals(1, result.size());
        verify(auditLogMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void listShouldReturnAllWhenNoFilters() {
        when(auditLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<AuditLog> result = auditLogService.list(null, null, null);

        assertNotNull(result);
        verify(auditLogMapper).selectList(any(LambdaQueryWrapper.class));
    }
}
