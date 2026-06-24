package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.auth.dto.AuthDtos.OAuthLoginRequest;
import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppleOAuthUserClient implements OAuthUserClient {
    private final ObjectMapper objectMapper;
    private final OAuthClientProperties properties;

    @Override
    public boolean supports(OAuthProvider provider) {
        return provider == OAuthProvider.APPLE;
    }

    @Override
    public OAuthUserInfo verify(OAuthLoginRequest request) {
        OAuthClientProperties.Provider config = properties.apple();
        if (config == null || config.clientId() == null || config.clientId().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_CONFIGURATION_REQUIRED);
        }
        String identityToken = request.authorizationCode();
        String[] parts = identityToken.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
        try {
            Map<String, Object> header = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[0]), new TypeReference<>() {
            });
            Map<String, Object> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {
            });
            verifySignature(identityToken, parts, (String) header.get("kid"));
            verifyClaims(claims, config.clientId());
            String subject = (String) claims.get("sub");
            String email = (String) claims.get("email");
            if (subject == null || subject.isBlank()) {
                throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
            }
            return new OAuthUserInfo(OAuthProvider.APPLE, subject, email, "Apple 사용자", null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private void verifySignature(String identityToken, String[] parts, String keyId) throws Exception {
        AppleKeys keys = RestClient.create("https://appleid.apple.com")
                .get()
                .uri("/auth/keys")
                .retrieve()
                .body(AppleKeys.class);
        AppleKey key = keys.keys().stream()
                .filter(candidate -> candidate.kid().equals(keyId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED));
        RSAPublicKey publicKey = rsaPublicKey(key.n(), key.e());
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8));
        boolean verified = signature.verify(Base64.getUrlDecoder().decode(parts[2]));
        if (!verified) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private RSAPublicKey rsaPublicKey(String modulus, String exponent) throws Exception {
        BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(modulus));
        BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(exponent));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
    }

    private void verifyClaims(Map<String, Object> claims, String audience) {
        if (!"https://appleid.apple.com".equals(claims.get("iss")) || !audience.equals(claims.get("aud"))) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
        Number exp = (Number) claims.get("exp");
        if (exp == null || exp.longValue() < Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.OAUTH_VERIFICATION_FAILED);
        }
    }

    private record AppleKeys(List<AppleKey> keys) {
    }

    private record AppleKey(String kid, String n, String e) {
    }
}
