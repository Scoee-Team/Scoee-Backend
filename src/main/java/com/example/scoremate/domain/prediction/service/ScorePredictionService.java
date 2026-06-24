package com.example.scoremate.domain.prediction.service;

import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.prediction.dto.PredictionDtos.*;
import com.example.scoremate.domain.prediction.dto.PredictionStatus;
import com.example.scoremate.domain.prediction.entity.ScorePrediction;
import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoom;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomMatch;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomMatchRepository;
import com.example.scoremate.domain.predictionroom.service.PredictionRoomService;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScorePredictionService {
    private final CurrentUserProvider currentUserProvider;
    private final PredictionRoomService roomService;
    private final PredictionRoomMatchRepository roomMatchRepository;
    private final ScorePredictionRepository scorePredictionRepository;

    @Transactional(readOnly = true)
    public MyPredictionsResponse myPredictions(Long roomId) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomService.getRoom(roomId);
        roomService.ensureParticipant(roomId, user.getId());
        List<PredictionRoomMatch> roomMatches = roomMatchRepository.findByRoomId(roomId);
        Map<Long, ScorePrediction> predictions = scorePredictionRepository.findByRoomIdAndUserId(roomId, user.getId()).stream()
                .collect(Collectors.toMap(p -> p.getFootballMatch().getId(), Function.identity()));
        List<MyPredictionMatchResponse> matches = roomMatches.stream().map(roomMatch -> {
            FootballMatch match = roomMatch.getFootballMatch();
            ScorePrediction prediction = predictions.get(match.getId());
            boolean locked = roomMatch.isLocked(LocalDateTime.now());
            return new MyPredictionMatchResponse(
                    match.getId(),
                    match.getLeague().getName(),
                    TeamResponse.from(match.getHomeTeam()),
                    TeamResponse.from(match.getAwayTeam()),
                    match.getKickoffTime(),
                    roomMatch.getPredictionDeadline(),
                    locked,
                    status(prediction, locked, match.isFinished()),
                    prediction == null ? null : prediction.getPredictedHomeScore(),
                    prediction == null ? null : prediction.getPredictedAwayScore(),
                    new CrowdSummaryResponse(0, 0, 0)
            );
        }).toList();
        return new MyPredictionsResponse(room.getId(), room.getTitle(), room.getStatus(), matches);
    }

    @Transactional
    public SubmitPredictionsResponse submit(Long roomId, SubmitPredictionsRequest request) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomService.getRoom(roomId);
        if (room.getStatus() == PredictionRoomStatus.COMPLETED || room.getStatus() == PredictionRoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ROOM_LOCKED);
        }
        roomService.ensureParticipant(roomId, user.getId());
        for (ScorePredictionRequest predictionRequest : request.predictions()) {
            validateScore(predictionRequest.homeScore(), predictionRequest.awayScore());
            PredictionRoomMatch roomMatch = roomMatchRepository.findByRoomIdAndFootballMatchId(roomId, predictionRequest.matchId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_IN_ROOM));
            if (roomMatch.isLocked(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.PREDICTION_CLOSED);
            }
            ScorePrediction prediction = scorePredictionRepository.findByRoomIdAndUserIdAndFootballMatchId(roomId, user.getId(), predictionRequest.matchId())
                    .orElseGet(() -> ScorePrediction.builder()
                            .room(room)
                            .user(user)
                            .footballMatch(roomMatch.getFootballMatch())
                            .submittedAt(LocalDateTime.now())
                            .build());
            prediction.setPredictedHomeScore(predictionRequest.homeScore());
            prediction.setPredictedAwayScore(predictionRequest.awayScore());
            prediction.setDeviation(null);
            prediction.setUpdatedAt(LocalDateTime.now());
            scorePredictionRepository.save(prediction);
        }
        int total = roomMatchRepository.findByRoomId(roomId).size();
        long submitted = scorePredictionRepository.countByRoomIdAndUserId(roomId, user.getId());
        return new SubmitPredictionsResponse(roomId, submitted, total, submitted == total ? PredictionStatus.SUBMITTED : PredictionStatus.NOT_SUBMITTED);
    }

    private PredictionStatus status(ScorePrediction prediction, boolean locked, boolean finished) {
        if (finished) {
            return PredictionStatus.COMPLETED;
        }
        if (locked) {
            return PredictionStatus.LOCKED;
        }
        return prediction == null ? PredictionStatus.NOT_SUBMITTED : PredictionStatus.EDITABLE;
    }

    private void validateScore(int homeScore, int awayScore) {
        if (homeScore < 0 || awayScore < 0 || homeScore > 20 || awayScore > 20) {
            throw new BusinessException(ErrorCode.INVALID_SCORE);
        }
    }
}
