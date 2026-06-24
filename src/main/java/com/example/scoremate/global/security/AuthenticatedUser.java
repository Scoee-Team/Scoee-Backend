package com.example.scoremate.global.security;

import com.example.scoremate.domain.user.entity.UserRole;

public record AuthenticatedUser(Long userId, UserRole role) {
}
