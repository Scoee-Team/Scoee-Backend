package com.example.scoremate.domain.prediction.repository;

import com.example.scoremate.domain.prediction.entity.ScorePrediction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScorePredictionRepository extends JpaRepository<ScorePrediction, Long> {
    Optional<ScorePrediction> findByRoomIdAndUserIdAndFootballMatchId(Long roomId, Long userId, Long footballMatchId);
    List<ScorePrediction> findByRoomId(Long roomId);
    List<ScorePrediction> findByRoomIdAndUserId(Long roomId, Long userId);
    long countByRoomIdAndUserId(Long roomId, Long userId);
}
