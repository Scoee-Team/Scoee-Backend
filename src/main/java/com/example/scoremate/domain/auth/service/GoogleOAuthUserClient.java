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
public class GoogleOAuthUserClient implements OAuthUserClient {
    private final OAuthClientProperties properties;

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo verify(OAuthLoginRequest request) {
        OAuthClientProperties.Provider config = properties.google();
        validate(config);
        try {
            LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("code", request.authorizationCode());
            form.add("client_id", config.clientId());
            form.add("client_secret", config.clientSecret());
            form.add("redirect_uri", request.redirectUri());
            form.add("grant_type", "authorization_code");
            TokenResponse token = RestClient.create("https://oauth2.googleapis.com")
                    .post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            UserInfoResponse user = RestClient.create("https://www.googleapis.com")
                    .get()
                    .uri("/oauth2/v3/userinfo")
                    .header("Authorization", "Bearer " + token.access_token())
                    .retrieve()
                    .body(UserInfoResponse.class);
            return new OAuthUserInfo(OAuthProvider.GOOGLE, user.sub(), user.email(), user.name(), user.picture());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private void validate(OAuthClientProperties.Provider config) {
        if (config == null || isBlank(config.clientId()) || isBlank(config.clientSecret())) {
            throw new BusinessException(ErrorCode.OAUTH_CONFIGURATION_REQUIRED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record TokenResponse(String access_token) {
    }

    private record UserInfoResponse(String sub, String email, String name, String picture) {
    }
}
