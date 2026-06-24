package com.example.scoremate.domain.predictionroom.repository;

import com.example.scoremate.domain.predictionroom.entity.PredictionRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionRoomRepository extends JpaRepository<PredictionRoom, Long> {
    Optional<PredictionRoom> findByInviteCode(String inviteCode);
    boolean existsByInviteCode(String inviteCode);
}
