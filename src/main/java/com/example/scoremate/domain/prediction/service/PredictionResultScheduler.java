package com.example.scoremate.domain.prediction.service;

import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomMatchRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomRepository;
import com.example.scoremate.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PredictionResultScheduler {
    private final PredictionRoomRepository roomRepository;
    private final PredictionRoomMatchRepository roomMatchRepository;
    private final PredictionResultService predictionResultService;

    @Scheduled(fixedDelayString = "${scoremate.scheduler.calculate-results-delay-ms:300000}")
    public void calculateCompletedRooms() {
        roomRepository.findAll().stream()
                .filter(room -> room.getStatus() != PredictionRoomStatus.COMPLETED && room.getStatus() != PredictionRoomStatus.CANCELLED)
                .filter(room -> {
                    var matches = roomMatchRepository.findByRoomId(room.getId());
                    return !matches.isEmpty() && matches.stream().allMatch(match -> match.getFootballMatch().isFinished());
                })
                .forEach(room -> {
                    try {
                        predictionResultService.calculateAsAdmin(room.getId());
                    } catch (BusinessException e) {
                        log.warn("prediction result calculation skipped roomId={}, code={}", room.getId(), e.getErrorCode().getCode());
                    } catch (Exception e) {
                        log.error("prediction result calculation failed roomId={}", room.getId(), e);
                    }
                });
    }
}
