package com.example.scoremate.domain.predictionroom.repository;

import com.example.scoremate.domain.predictionroom.entity.PredictionRoomMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PredictionRoomMatchRepository extends JpaRepository<PredictionRoomMatch, Long> {
    List<PredictionRoomMatch> findByRoomId(Long roomId);
    List<PredictionRoomMatch> findByFootballMatchId(Long footballMatchId);
    Optional<PredictionRoomMatch> findByRoomIdAndFootballMatchId(Long roomId, Long footballMatchId);
    long countByFootballMatchId(Long footballMatchId);
}
