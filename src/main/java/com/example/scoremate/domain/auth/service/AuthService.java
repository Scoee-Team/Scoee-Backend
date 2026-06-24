package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.auth.dto.AuthDtos.AuthTokenResponse;
import com.example.scoremate.domain.auth.dto.AuthDtos.OAuthLoginRequest;
import com.example.scoremate.domain.auth.dto.AuthDtos.TokenRefreshRequest;
import com.example.scoremate.domain.auth.entity.RefreshToken;
import com.example.scoremate.domain.auth.repository.RefreshTokenRepository;
import com.example.scoremate.domain.notification.entity.NotificationSetting;
import com.example.scoremate.domain.notification.repository.NotificationSettingRepository;
import com.example.scoremate.domain.user.dto.UserDtos.UserResponse;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.entity.UserRole;
import com.example.scoremate.domain.user.repository.UserRepository;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.JwtTokenProvider;
import com.example.scoremate.global.security.CurrentUserProvider;
import com.example.scoremate.global.util.HashUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final CurrentUserProvider currentUserProvider;
    private final List<OAuthUserClient> oauthUserClients;

    @Transactional
    public AuthTokenResponse login(OAuthLoginRequest request) {
        OAuthUserInfo userInfo = verifyOAuthUser(request);
        boolean[] created = {false};
        User user = userRepository.findByProviderAndProviderSubject(userInfo.provider(), userInfo.providerSubject())
                .orElseGet(() -> {
                    created[0] = true;
                    User saved = userRepository.save(User.builder()
                            .email(userInfo.email())
                            .nickname(defaultNickname(userInfo))
                            .statusMessage("")
                            .profileImageUrl(userInfo.profileImageUrl())
                            .role(UserRole.USER)
                            .provider(userInfo.provider())
                            .providerSubject(userInfo.providerSubject())
                            .build());
                    notificationSettingRepository.save(NotificationSetting.builder()
                            .user(saved)
                            .deadlineReminder(true)
                            .matchResult(true)
                            .friendJoined(true)
                            .build());
                    return saved;
                });
        user.setEmail(userInfo.email());
        user.setNickname(defaultNickname(userInfo));
        user.setProfileImageUrl(userInfo.profileImageUrl());

        return tokenResponse(user, created[0]);
    }

    @Transactional
    public AuthTokenResponse refresh(TokenRefreshRequest request) {
        Long userId = jwtTokenProvider.parseRefreshToken(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(HashUtils.sha256(request.refreshToken()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now()) || !refreshToken.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        refreshToken.setRevoked(true);
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return tokenResponse(user, false);
    }

    @Transactional
    public void logout() {
        User user = currentUserProvider.getCurrentUser();
        refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())
                .forEach(token -> token.setRevoked(true));
    }

    private AuthTokenResponse tokenResponse(User user, boolean isNewUser) {
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(HashUtils.sha256(refreshToken))
                .expiresAt(LocalDateTime.now().plusSeconds(jwtTokenProvider.refreshTokenTtlSeconds()))
                .revoked(false)
                .build());
        return new AuthTokenResponse(
                jwtTokenProvider.createAccessToken(user),
                refreshToken,
                "Bearer",
                jwtTokenProvider.accessTokenTtlSeconds(),
                isNewUser,
                UserResponse.from(user)
        );
    }

    private OAuthUserInfo verifyOAuthUser(OAuthLoginRequest request) {
        return oauthUserClients.stream()
                .filter(client -> client.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_OAUTH_PROVIDER))
                .verify(request);
    }

    private String defaultNickname(OAuthUserInfo userInfo) {
        if (userInfo.nickname() != null && !userInfo.nickname().isBlank()) {
            return userInfo.nickname().length() > 12 ? userInfo.nickname().substring(0, 12) : userInfo.nickname();
        }
        return "사용자" + userInfo.providerSubject().substring(0, Math.min(4, userInfo.providerSubject().length()));
    }
}
