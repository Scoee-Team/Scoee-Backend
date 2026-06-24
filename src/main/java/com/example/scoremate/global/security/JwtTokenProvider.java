package com.example.scoremate.global.security;

import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.entity.UserRole;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtTokenProvider {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long accessTokenTtlSeconds;
    private final long refreshTokenTtlSeconds;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${scoremate.jwt.secret:change-this-secret-in-production}") String secret,
            @Value("${scoremate.jwt.access-token-ttl-seconds:3600}") long accessTokenTtlSeconds,
            @Value("${scoremate.jwt.refresh-token-ttl-seconds:1209600}") long refreshTokenTtlSeconds
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }

    public String createAccessToken(User user) {
        return createToken(user, accessTokenTtlSeconds, "access");
    }

    public String createRefreshToken(User user) {
        return createToken(user, refreshTokenTtlSeconds, "refresh");
    }

    public long accessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public AuthenticatedUser parseAccessToken(String token) {
        Map<String, Object> claims = parseClaims(token);
        if (!"access".equals(claims.get("typ"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return new AuthenticatedUser(((Number) claims.get("sub")).longValue(), UserRole.valueOf((String) claims.get("role")));
    }

    public Long parseRefreshToken(String token) {
        Map<String, Object> claims = parseClaims(token);
        if (!"refresh".equals(claims.get("typ"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return ((Number) claims.get("sub")).longValue();
    }

    private String createToken(User user, long ttlSeconds, String type) {
        try {
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of(
                    "sub", user.getId(),
                    "role", user.getRole().name(),
                    "typ", type,
                    "iat", Instant.now().getEpochSecond(),
                    "exp", Instant.now().plusSeconds(ttlSeconds).getEpochSecond()
            ));
            String unsigned = header + "." + payload;
            return unsigned + "." + sign(unsigned);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> parseClaims(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            String unsigned = parts[0] + "." + parts[1];
            if (!constantTimeEquals(sign(unsigned), parts[2])) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            Map<String, Object> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), new TypeReference<>() {
            });
            Number exp = (Number) claims.get("exp");
            if (exp == null || exp.longValue() < Instant.now().getEpochSecond()) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            return claims;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String encodeJson(Map<String, Object> value) throws JsonProcessingException {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.equals(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }
}
