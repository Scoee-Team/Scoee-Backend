package com.example.scoremate.domain.prediction.repository;

import com.example.scoremate.domain.prediction.entity.PredictionRoomResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PredictionRoomResultRepository extends JpaRepository<PredictionRoomResult, Long> {
    Optional<PredictionRoomResult> findByRoomId(Long roomId);
}
