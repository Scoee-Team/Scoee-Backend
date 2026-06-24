package com.example.scoremate.domain.predictionroom.repository;

import com.example.scoremate.domain.predictionroom.entity.PredictionRoomParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRoomParticipantRepository extends JpaRepository<PredictionRoomParticipant, Long> {
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
    Optional<PredictionRoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);
    List<PredictionRoomParticipant> findByRoomId(Long roomId);
    Page<PredictionRoomParticipant> findByUserId(Long userId, Pageable pageable);
    long countByRoomId(Long roomId);
}
