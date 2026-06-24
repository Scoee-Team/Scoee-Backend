package com.example.scoremate.domain.notification.service;

import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationResponse;
import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationSettingRequest;
import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationSettingResponse;
import com.example.scoremate.domain.notification.entity.Notification;
import com.example.scoremate.domain.notification.entity.NotificationType;
import com.example.scoremate.domain.notification.entity.NotificationSetting;
import com.example.scoremate.domain.notification.repository.NotificationRepository;
import com.example.scoremate.domain.notification.repository.NotificationSettingRepository;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.response.PageResponse;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final CurrentUserProvider currentUserProvider;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingRepository settingRepository;

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> notifications(Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        return PageResponse.from(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse));
    }

    @Transactional
    public void read(Long notificationId) {
        User user = currentUserProvider.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        notification.setRead(true);
    }

    @Transactional
    public void readAll() {
        User user = currentUserProvider.getCurrentUser();
        notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), Pageable.unpaged())
                .forEach(notification -> notification.setRead(true));
    }

    @Transactional
    public NotificationSettingResponse getSetting() {
        User user = currentUserProvider.getCurrentUser();
        NotificationSetting setting = getOrCreateSetting(user);
        return new NotificationSettingResponse(setting.isDeadlineReminder(), setting.isMatchResult(), setting.isFriendJoined());
    }

    @Transactional
    public NotificationSettingResponse updateSetting(NotificationSettingRequest request) {
        User user = currentUserProvider.getCurrentUser();
        NotificationSetting setting = getOrCreateSetting(user);
        if (request.deadlineReminder() != null) {
            setting.setDeadlineReminder(request.deadlineReminder());
        }
        if (request.matchResult() != null) {
            setting.setMatchResult(request.matchResult());
        }
        if (request.friendJoined() != null) {
            setting.setFriendJoined(request.friendJoined());
        }
        return new NotificationSettingResponse(setting.isDeadlineReminder(), setting.isMatchResult(), setting.isFriendJoined());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void create(User user, NotificationType type, String title, String body, String deepLink) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .read(false)
                .deepLink(deepLink)
                .build());
    }

    private NotificationSetting getOrCreateSetting(User user) {
        return settingRepository.findByUserId(user.getId())
                .orElseGet(() -> settingRepository.save(NotificationSetting.builder()
                        .user(user)
                        .deadlineReminder(true)
                        .matchResult(true)
                        .friendJoined(true)
                        .build()));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getType(), notification.getTitle(), notification.getBody(), notification.isRead(), notification.getCreatedAt(), notification.getDeepLink());
    }
}
