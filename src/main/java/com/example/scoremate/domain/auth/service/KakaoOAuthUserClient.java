package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.auth.dto.AuthDtos.OAuthLoginRequest;
import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoOAuthUserClient implements OAuthUserClient {
    private final OAuthClientProperties properties;

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo verify(OAuthLoginRequest request) {
        OAuthClientProperties.Provider config = properties.kakao();
        validate(config);
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", config.clientId());
            if (!isBlank(config.clientSecret())) {
                form.add("client_secret", config.clientSecret());
            }
            form.add("redirect_uri", request.redirectUri());
            form.add("code", request.authorizationCode());
            TokenResponse token = RestClient.create("https://kauth.kakao.com")
                    .post()
                    .uri("/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            UserInfoResponse user = RestClient.create("https://kapi.kakao.com")
                    .get()
                    .uri("/v2/user/me")
                    .header("Authorization", "Bearer " + token.access_token())
                    .retrieve()
                    .body(UserInfoResponse.class);
            String nickname = user.properties() == null ? null : user.properties().nickname();
            String profileImage = user.properties() == null ? null : user.properties().profile_image();
            String email = user.kakao_account() == null ? null : user.kakao_account().email();
            return new OAuthUserInfo(OAuthProvider.KAKAO, String.valueOf(user.id()), email, nickname, profileImage);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private void validate(OAuthClientProperties.Provider config) {
        if (config == null || isBlank(config.clientId())) {
            throw new BusinessException(ErrorCode.OAUTH_CONFIGURATION_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TokenResponse(String access_token) {
    }

    private record UserInfoResponse(Long id, KakaoAccount kakao_account, KakaoProperties properties) {
    }

    private record KakaoAccount(String email) {
    }

    private record KakaoProperties(String nickname, String profile_image) {
    }
}
