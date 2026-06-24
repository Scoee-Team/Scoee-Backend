package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.auth.dto.AuthDtos.AuthTokenResponse;
import com.example.scoremate.domain.auth.dto.AuthDtos.OAuthLoginRequest;
import com.example.scoremate.domain.auth.dto.AuthDtos.TokenRefreshRequest;
import com.example.scoremate.domain.notification.entity.NotificationSetting;
import com.example.scoremate.domain.notification.repository.NotificationSettingRepository;
import com.example.scoremate.domain.user.dto.UserDtos.UserResponse;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.entity.UserRole;
import com.example.scoremate.domain.user.repository.UserRepository;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthTokenResponse login(OAuthLoginRequest request) {
        String providerSubject = request.authorizationCode();
        boolean[] created = {false};
        User user = userRepository.findByProviderAndProviderSubject(request.provider(), providerSubject)
                .orElseGet(() -> {
                    created[0] = true;
                    User saved = userRepository.save(User.builder()
                            .email(request.provider().name().toLowerCase() + "_" + providerSubject + "@oauth.local")
                            .nickname("사용자" + providerSubject.substring(0, Math.min(4, providerSubject.length())))
                            .statusMessage("")
                            .role(UserRole.USER)
                            .provider(request.provider())
                            .providerSubject(providerSubject)
                            .build());
                    notificationSettingRepository.save(NotificationSetting.builder()
                            .user(saved)
                            .deadlineReminder(true)
                            .matchResult(true)
                            .friendJoined(true)
                            .build());
                    return saved;
                });

        return tokenResponse(user, created[0]);
    }

    @Transactional(readOnly = true)
    public AuthTokenResponse refresh(TokenRefreshRequest request) {
        if (!request.refreshToken().startsWith("refresh-token-")) {
            Long userId = jwtTokenProvider.parseRefreshToken(request.refreshToken());
            User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            return tokenResponse(user, false);
        }
        Long userId = parseId(request.refreshToken().substring("refresh-token-".length()));
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return tokenResponse(user, false);
    }

    public void logout() {
    }

    private AuthTokenResponse tokenResponse(User user, boolean isNewUser) {
        return new AuthTokenResponse(
                jwtTokenProvider.createAccessToken(user),
                jwtTokenProvider.createRefreshToken(user),
                "Bearer",
                jwtTokenProvider.accessTokenTtlSeconds(),
                isNewUser,
                UserResponse.from(user)
        );
    }

    private Long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
