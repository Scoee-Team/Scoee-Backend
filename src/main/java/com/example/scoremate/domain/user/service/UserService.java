package com.example.scoremate.domain.user.service;

import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomParticipantRepository;
import com.example.scoremate.domain.user.dto.UserDtos.ProfileImageResponse;
import com.example.scoremate.domain.user.dto.UserDtos.UpdateProfileRequest;
import com.example.scoremate.domain.user.dto.UserDtos.UserProfileResponse;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
public class UserService {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp");
    private final CurrentUserProvider currentUserProvider;
    private final PredictionRoomParticipantRepository participantRepository;
    private final ScorePredictionRepository scorePredictionRepository;
    private final Path profileImageDir;

    public UserService(
            CurrentUserProvider currentUserProvider,
            PredictionRoomParticipantRepository participantRepository,
            ScorePredictionRepository scorePredictionRepository,
            @Value("${scoremate.storage.profile-image-dir:uploads/profile-images}") String profileImageDir
    ) {
        this.currentUserProvider = currentUserProvider;
        this.participantRepository = participantRepository;
        this.scorePredictionRepository = scorePredictionRepository;
        this.profileImageDir = Path.of(profileImageDir);
    }

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
        if (file == null || file.isEmpty() || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new com.example.scoremate.global.exception.BusinessException(com.example.scoremate.global.exception.ErrorCode.INVALID_INPUT);
        }
        String extension = extension(file.getOriginalFilename());
        String filename = user.getId() + "-" + UUID.randomUUID() + extension;
        try {
            Files.createDirectories(profileImageDir);
            Files.copy(file.getInputStream(), profileImageDir.resolve(filename));
        } catch (IOException e) {
            throw new com.example.scoremate.global.exception.BusinessException(com.example.scoremate.global.exception.ErrorCode.INTERNAL_SERVER_ERROR);
        }
        user.setProfileImageUrl("/uploads/profile-images/" + filename);
        return new ProfileImageResponse(user.getProfileImageUrl());
    }

    @Transactional
    public void resetProfileImage() {
        currentUserProvider.getCurrentUser().setProfileImageUrl(null);
    }

    private String extension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        return ext.length() > 10 ? "" : ext;
    }
}
