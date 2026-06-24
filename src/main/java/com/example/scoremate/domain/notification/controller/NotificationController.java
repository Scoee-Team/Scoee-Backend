package com.example.scoremate.domain.notification.controller;

import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationResponse;
import com.example.scoremate.domain.notification.service.NotificationService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @Operation(summary = "알림 목록", description = "AUTH_REQUIRED")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> notifications(Pageable pageable) {
        return ApiResponse.success(notificationService.notifications(pageable));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> read(@PathVariable Long notificationId) {
        notificationService.read(notificationId);
        return ApiResponse.success();
    }

    @Operation(summary = "모든 알림 읽음 처리")
    @PatchMapping("/read-all")
    public ApiResponse<Void> readAll() {
        notificationService.readAll();
        return ApiResponse.success();
    }
}
