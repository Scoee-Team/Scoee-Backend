package com.example.scoremate.domain.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scoremate.oauth")
public record OAuthClientProperties(
        Provider kakao,
        Provider google,
        Provider apple
) {
    public record Provider(String clientId, String clientSecret, String teamId, String keyId, String privateKey) {
    }
}
