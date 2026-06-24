package com.example.scoremate.global.security;

import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.repository.UserRepository;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final HttpServletRequest request;
    private final UserRepository userRepository;

    public User getCurrentUser() {
        Long userId = resolveUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    public Long resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser.userId();
        }
        String headerUserId = request.getHeader("X-USER-ID");
        if (headerUserId != null && !headerUserId.isBlank()) {
            return parseUserId(headerUserId);
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer access-token-")) {
            return parseUserId(authorization.substring("Bearer access-token-".length()));
        }
        throw new BusinessException(ErrorCode.AUTH_REQUIRED);
    }

    private Long parseUserId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
