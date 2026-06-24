package com.example.scoremate.domain.notification.dto;

import com.example.scoremate.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public final class NotificationDtos {
    private NotificationDtos() {
    }

    public record NotificationResponse(
            Long id,
            NotificationType type,
            String title,
            String body,
            boolean read,
            LocalDateTime createdAt,
            String deepLink
    ) {
    }

    public record NotificationSettingRequest(Boolean deadlineReminder, Boolean matchResult, Boolean friendJoined) {
    }

    public record NotificationSettingResponse(boolean deadlineReminder, boolean matchResult, boolean friendJoined) {
    }
}
