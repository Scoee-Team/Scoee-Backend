package com.example.scoremate.domain.user.service;

import com.example.scoremate.domain.prediction.repository.PredictionRoomResultRepository;
import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomParticipantRepository;
import com.example.scoremate.domain.user.dto.UserDtos.ProfileImageResponse;
import com.example.scoremate.domain.user.dto.UserDtos.UpdateProfileRequest;
import com.example.scoremate.domain.user.dto.UserDtos.UserProfileResponse;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {
    private final CurrentUserProvider currentUserProvider;
    private final PredictionRoomParticipantRepository participantRepository;
    private final ScorePredictionRepository scorePredictionRepository;
    private final PredictionRoomResultRepository resultRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse me() {
        User user = currentUserProvider.getCurrentUser();
        long activeRooms = participantRepository.findByUserId(user.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(p -> p.getRoom().getStatus() != com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus.COMPLETED).count();
        double avgDeviation = scorePredictionRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()) && p.getDeviation() != null)
                .mapToInt(p -> p.getDeviation()).average().orElse(0);
        return new UserProfileResponse(user.getId(), user.getNickname(), user.getStatusMessage(), user.getProfileImageUrl(), (int) activeRooms, avgDeviation, user.getProvider());
    }

    @Transactional
    public UserProfileResponse update(UpdateProfileRequest request) {
        User user = currentUserProvider.getCurrentUser();
        if (request.nickname() != null) {
            user.setNickname(request.nickname());
        }
        if (request.statusMessage() != null) {
            user.setStatusMessage(request.statusMessage());
        }
        return me();
    }

    @Transactional
    public ProfileImageResponse uploadProfileImage(MultipartFile file) {
        User user = currentUserProvider.getCurrentUser();
        user.setProfileImageUrl("/uploads/users/" + user.getId() + "/" + file.getOriginalFilename());
        return new ProfileImageResponse(user.getProfileImageUrl());
    }

    @Transactional
    public void resetProfileImage() {
        currentUserProvider.getCurrentUser().setProfileImageUrl(null);
    }
}
