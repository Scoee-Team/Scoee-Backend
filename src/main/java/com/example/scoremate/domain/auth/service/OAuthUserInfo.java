package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.user.entity.OAuthProvider;

public record OAuthUserInfo(
        OAuthProvider provider,
        String providerSubject,
        String email,
        String nickname,
        String profileImageUrl
) {
}
