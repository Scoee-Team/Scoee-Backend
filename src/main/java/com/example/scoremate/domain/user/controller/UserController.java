package com.example.scoremate.domain.user.controller;

import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationSettingRequest;
import com.example.scoremate.domain.notification.dto.NotificationDtos.NotificationSettingResponse;
import com.example.scoremate.domain.notification.service.NotificationService;
import com.example.scoremate.domain.user.dto.UserDtos.*;
import com.example.scoremate.domain.user.service.UserService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Users")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService userService;
    private final NotificationService notificationService;

    @Operation(summary = "내 정보 조회", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.success(userService.me());
    }

    @Operation(summary = "내 프로필 수정", description = "INVALID_INPUT")
    @PatchMapping
    public ApiResponse<UserProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.update(request));
    }

    @Operation(summary = "프로필 이미지 업로드", description = "현재 MVP는 파일 저장소 연동 전 URL만 생성합니다.")
    @PostMapping("/profile-image")
    public ApiResponse<ProfileImageResponse> upload(@RequestPart MultipartFile file) {
        return ApiResponse.success(userService.uploadProfileImage(file));
    }

    @Operation(summary = "기본 프로필 이미지로 변경")
    @DeleteMapping("/profile-image")
    public ApiResponse<Void> resetImage() {
        userService.resetProfileImage();
        return ApiResponse.success();
    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> notificationSettings() {
        return ApiResponse.success(notificationService.getSetting());
    }

    @Operation(summary = "알림 설정 수정")
    @PatchMapping("/notification-settings")
    public ApiResponse<NotificationSettingResponse> updateNotificationSettings(@RequestBody NotificationSettingRequest request) {
        return ApiResponse.success(notificationService.updateSetting(request));
    }
}
