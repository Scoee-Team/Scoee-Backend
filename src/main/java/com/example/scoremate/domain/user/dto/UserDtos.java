package com.example.scoremate.domain.user.dto;

import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.domain.user.entity.User;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String nickname,
            String statusMessage,
            String profileImageUrl,
            OAuthProvider provider
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getNickname(), user.getStatusMessage(), user.getProfileImageUrl(), user.getProvider());
        }
    }

    public record UserProfileResponse(
            Long id,
            String nickname,
            String statusMessage,
            String profileImageUrl,
            int activeRoomCount,
            double averageDeviation,
            OAuthProvider provider
    ) {
    }

    public record UpdateProfileRequest(
            @Size(min = 2, max = 12, message = "닉네임은 2~12자여야 합니다.") String nickname,
            @Size(max = 40, message = "상태 메시지는 40자 이하여야 합니다.") String statusMessage
    ) {
    }

    public record ProfileImageResponse(String profileImageUrl) {
    }
}
