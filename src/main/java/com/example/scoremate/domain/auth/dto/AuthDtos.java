package com.example.scoremate.domain.auth.dto;

import com.example.scoremate.domain.user.dto.UserDtos.UserResponse;
import com.example.scoremate.domain.user.entity.OAuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record OAuthLoginRequest(
            @NotNull(message = "provider는 필수입니다.") OAuthProvider provider,
            @NotBlank(message = "authorizationCode는 필수입니다.") String authorizationCode,
            String redirectUri
    ) {
    }

    public record TokenRefreshRequest(@NotBlank(message = "refreshToken은 필수입니다.") String refreshToken) {
    }

    public record AuthTokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            boolean isNewUser,
            UserResponse user
    ) {
    }
}
