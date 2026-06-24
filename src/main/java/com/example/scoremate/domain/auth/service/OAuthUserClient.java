package com.example.scoremate.domain.auth.service;

import com.example.scoremate.domain.auth.dto.AuthDtos.OAuthLoginRequest;
import com.example.scoremate.domain.user.entity.OAuthProvider;

public interface OAuthUserClient {
    boolean supports(OAuthProvider provider);
    OAuthUserInfo verify(OAuthLoginRequest request);
}
