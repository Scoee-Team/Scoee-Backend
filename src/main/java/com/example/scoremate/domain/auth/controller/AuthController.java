package com.example.scoremate.domain.auth.controller;

import com.example.scoremate.domain.auth.dto.AuthDtos.*;
import com.example.scoremate.domain.auth.service.AuthService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "OAuth 로그인과 토큰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "OAuth 로그인", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @PostMapping("/oauth/login")
    public ApiResponse<AuthTokenResponse> login(@Valid @RequestBody OAuthLoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @Operation(summary = "토큰 재발급", description = "INVALID_TOKEN, USER_NOT_FOUND")
    @PostMapping("/token/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @Operation(summary = "로그아웃", description = "현재 MVP에서는 서버 저장 토큰이 없으므로 성공 응답만 반환합니다.")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }
}
