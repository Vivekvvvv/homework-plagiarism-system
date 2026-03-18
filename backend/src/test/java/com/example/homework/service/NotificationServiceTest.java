package com.example.homework.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.homework.common.exception.BusinessException;
import com.example.homework.domain.entity.SysUser;
import com.example.homework.domain.entity.UserNotification;
import com.example.homework.mapper.SysUserMapper;
import com.example.homework.mapper.UserNotificationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationMapper userNotificationMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private NotificationWebSocketHandler webSocketHandler;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
            userNotificationMapper, sysUserMapper, webSocketHandler);
    }

    // --- createNotification ---

    @Test
    void createNotificationShouldInsertAndPushViaWebSocket() {
        SysUser targetUser = new SysUser();
        targetUser.setId(5L);
        targetUser.setUsername("student1");
        when(sysUserMapper.selectById(5L)).thenReturn(targetUser);
        when(userNotificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        UserNotification result = notificationService.createNotification(
            5L, "Test Title", "Test Content", "info", "review", "1");

        assertNotNull(result);
        assertEquals(5L, result.getUserId());
        assertEquals("Test Title", result.getTitle());
        assertEquals("Test Content", result.getContent());
        assertEquals("info", result.getLevel());
        assertEquals(0, result.getStatus());
        verify(userNotificationMapper).insert(any(UserNotification.class));
        verify(webSocketHandler).sendToUser(eq("student1"), any(UserNotification.class));
    }

    @Test
    void createNotificationShouldNotFailWhenWebSocketPushFails() {
        SysUser targetUser = new SysUser();
        targetUser.setId(5L);
        targetUser.setUsername("student1");
        when(sysUserMapper.selectById(5L)).thenReturn(targetUser);
        when(userNotificationMapper.insert(any(UserNotification.class))).thenReturn(1);
        doThrow(new RuntimeException("ws error"))
            .when(webSocketHandler).sendToUser(any(), any(UserNotification.class));

        UserNotification result = notificationService.createNotification(
            5L, "Title", "Content", "info", "test", "1");

        assertNotNull(result);
        verify(userNotificationMapper).insert(any(UserNotification.class));
    }

    @Test
    void createNotificationShouldThrowWhenUserIdIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> notificationService.createNotification(null, "Title", "Content", "info", "test", "1"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createNotificationShouldThrowWhenTitleIsEmpty() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> notificationService.createNotification(5L, "", "Content", "info", "test", "1"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void createNotificationShouldNormalizeLevels() {
        when(sysUserMapper.selectById(5L)).thenReturn(null);
        when(userNotificationMapper.insert(any(UserNotification.class))).thenReturn(1);

        // "warn" -> "warning"
        UserNotification n1 = notificationService.createNotification(
            5L, "T", "C", "warn", "test", "1");
        assertEquals("warning", n1.getLevel());

        // "error" -> "danger"
        UserNotification n2 = notificationService.createNotification(
            5L, "T", "C", "error", "test", "1");
        assertEquals("danger", n2.getLevel());

        // null -> "info"
        UserNotification n3 = notificationService.createNotification(
            5L, "T", "C", null, "test", "1");
        assertEquals("info", n3.getLevel());

        // "success" -> "success"
        UserNotification n4 = notificationService.createNotification(
            5L, "T", "C", "success", "test", "1");
        assertEquals("success", n4.getLevel());
    }

    // --- list ---

    @Test
    @SuppressWarnings("unchecked")
    void listShouldReturnNotificationsForUser() {
        SysUser actor = buildUser(5L);
        UserNotification n1 = new UserNotification();
        n1.setId(1L);
        when(userNotificationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(n1));

        List<UserNotification> result = notificationService.list(actor, null, null);

        assertEquals(1, result.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listShouldFilterByStatus() {
        SysUser actor = buildUser(5L);
        when(userNotificationMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        List<UserNotification> result = notificationService.list(actor, 0, 10);

        assertNotNull(result);
        verify(userNotificationMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // --- markRead ---

    @Test
    @SuppressWarnings("unchecked")
    void markReadShouldUpdateSpecificIds() {
        SysUser actor = buildUser(5L);
        when(userNotificationMapper.update(any(UserNotification.class), any(LambdaQueryWrapper.class))).thenReturn(2);

        int count = notificationService.markRead(actor, false, List.of(1L, 2L));

        assertEquals(2, count);
    }

    @Test
    @SuppressWarnings("unchecked")
    void markReadAllShouldUpdateAll() {
        SysUser actor = buildUser(5L);
        when(userNotificationMapper.update(any(UserNotification.class), any(LambdaQueryWrapper.class))).thenReturn(5);

        int count = notificationService.markRead(actor, true, null);

        assertEquals(5, count);
    }

    @Test
    void markReadShouldThrowWhenIdsEmptyAndNotAll() {
        SysUser actor = buildUser(5L);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> notificationService.markRead(actor, false, List.of()));
        assertEquals(400, ex.getCode());
    }

    // --- helpers ---

    private SysUser buildUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user" + id);
        return user;
    }
}
