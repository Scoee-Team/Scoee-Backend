package com.example.scoremate.domain.prediction.service;

import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.notification.entity.NotificationType;
import com.example.scoremate.domain.notification.service.NotificationService;
import com.example.scoremate.domain.prediction.dto.PredictionDtos.*;
import com.example.scoremate.domain.prediction.entity.PredictionRoomResult;
import com.example.scoremate.domain.prediction.entity.ScorePrediction;
import com.example.scoremate.domain.prediction.repository.PredictionRoomResultRepository;
import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoom;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomMatch;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomParticipant;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomMatchRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomParticipantRepository;
import com.example.scoremate.domain.predictionroom.service.PredictionRoomService;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionResultService {
    private final CurrentUserProvider currentUserProvider;
    private final PredictionRoomService roomService;
    private final PredictionRoomMatchRepository roomMatchRepository;
    private final PredictionRoomParticipantRepository participantRepository;
    private final ScorePredictionRepository scorePredictionRepository;
    private final PredictionRoomResultRepository resultRepository;
    private final DeviationCalculator deviationCalculator;
    private final NotificationService notificationService;

    @Transactional
    public PredictionRoomResultResponse calculateAndGet(Long roomId) {
        User current = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomService.getRoom(roomId);
        roomService.ensureParticipant(roomId, current.getId());
        return calculate(room);
    }

    @Transactional
    public PredictionRoomResultResponse calculateAsAdmin(Long roomId) {
        PredictionRoom room = roomService.getRoom(roomId);
        return calculate(room);
    }

    private PredictionRoomResultResponse calculate(PredictionRoom room) {
        Long roomId = room.getId();
        List<PredictionRoomMatch> roomMatches = roomMatchRepository.findByRoomId(roomId);
        if (roomMatches.isEmpty() || roomMatches.stream().anyMatch(m -> !m.getFootballMatch().isFinished())) {
            throw new BusinessException(ErrorCode.RESULT_NOT_READY);
        }
        List<ParticipantStats> stats = calculateStats(room, roomMatches);
        List<ParticipantStats> loserStats = selectLosers(stats);
        ParticipantStats primaryLoser = loserStats.get(0);
        Optional<PredictionRoomResult> existingResult = resultRepository.findByRoomId(roomId);
        boolean firstCalculation = existingResult.isEmpty();
        PredictionRoomResult result = existingResult
                .orElseGet(() -> PredictionRoomResult.builder().room(room).build());
        result.setLoser(primaryLoser.user());
        result.setLoserTotalDeviation(primaryLoser.totalDeviation());
        result.setCalculatedAt(LocalDateTime.now());
        resultRepository.save(result);
        room.setStatus(PredictionRoomStatus.COMPLETED);
        if (firstCalculation) {
            notifyResult(room, participantRepository.findByRoomId(roomId), loserStats);
        }
        return toResponse(room, roomMatches, stats, loserStats);
    }

    @Transactional(readOnly = true)
    public PredictionRoomResultResponse get(Long roomId) {
        User current = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomService.getRoom(roomId);
        roomService.ensureParticipant(roomId, current.getId());
        List<PredictionRoomMatch> roomMatches = roomMatchRepository.findByRoomId(roomId);
        if (roomMatches.isEmpty() || roomMatches.stream().anyMatch(m -> !m.getFootballMatch().isFinished())) {
            throw new BusinessException(ErrorCode.RESULT_NOT_READY);
        }
        List<ParticipantStats> stats = calculateStats(room, roomMatches);
        return toResponse(room, roomMatches, stats, selectLosers(stats));
    }

    public List<ParticipantStats> calculateStats(PredictionRoom room, List<PredictionRoomMatch> roomMatches) {
        List<PredictionRoomParticipant> participants = participantRepository.findByRoomId(room.getId());
        Map<String, ScorePrediction> predictions = scorePredictionRepository.findByRoomId(room.getId()).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId() + ":" + p.getFootballMatch().getId(), Function.identity()));
        List<ParticipantStats> stats = new ArrayList<>();
        for (PredictionRoomParticipant participant : participants) {
            int totalDeviation = 0;
            int missingCount = 0;
            int maxSingleDeviation = 0;
            int submittedCount = 0;
            LocalDateTime lastSubmittedAt = null;
            for (PredictionRoomMatch roomMatch : roomMatches) {
                FootballMatch match = roomMatch.getFootballMatch();
                ScorePrediction prediction = predictions.get(participant.getUser().getId() + ":" + match.getId());
                int deviation;
                if (prediction == null) {
                    deviation = deviationCalculator.missingPredictionPenalty();
                    missingCount++;
                } else {
                    deviation = deviationCalculator.calculate(
                            prediction.getPredictedHomeScore(),
                            prediction.getPredictedAwayScore(),
                            match.getHomeScore(),
                            match.getAwayScore());
                    prediction.setDeviation(deviation);
                    submittedCount++;
                    if (lastSubmittedAt == null || prediction.getUpdatedAt().isAfter(lastSubmittedAt)) {
                        lastSubmittedAt = prediction.getUpdatedAt();
                    }
                }
                totalDeviation += deviation;
                maxSingleDeviation = Math.max(maxSingleDeviation, deviation);
            }
            stats.add(new ParticipantStats(participant.getUser(), totalDeviation, missingCount, maxSingleDeviation, lastSubmittedAt, submittedCount));
        }
        stats.sort(Comparator.comparingInt(ParticipantStats::totalDeviation));
        return stats;
    }

    public List<ParticipantStats> selectLosers(List<ParticipantStats> stats) {
        if (stats.isEmpty()) {
            return List.of();
        }
        int maxTotalDeviation = stats.stream().mapToInt(ParticipantStats::totalDeviation).max().orElse(0);
        List<ParticipantStats> candidates = stats.stream().filter(s -> s.totalDeviation() == maxTotalDeviation).toList();
        int maxMissing = candidates.stream().mapToInt(ParticipantStats::missingPredictionCount).max().orElse(0);
        candidates = candidates.stream().filter(s -> s.missingPredictionCount() == maxMissing).toList();
        int maxSingle = candidates.stream().mapToInt(ParticipantStats::maxSingleDeviation).max().orElse(0);
        candidates = candidates.stream().filter(s -> s.maxSingleDeviation() == maxSingle).toList();
        Optional<LocalDateTime> latest = candidates.stream().map(ParticipantStats::lastSubmittedAt).filter(Objects::nonNull).max(Comparator.naturalOrder());
        if (latest.isPresent()) {
            candidates = candidates.stream().filter(s -> latest.get().equals(s.lastSubmittedAt())).toList();
        }
        return candidates;
    }

    private PredictionRoomResultResponse toResponse(PredictionRoom room, List<PredictionRoomMatch> roomMatches, List<ParticipantStats> stats, List<ParticipantStats> losers) {
        Set<Long> loserIds = losers.stream().map(s -> s.user().getId()).collect(Collectors.toSet());
        int bestDeviation = stats.stream().mapToInt(ParticipantStats::totalDeviation).min().orElse(0);
        List<ParticipantResultResponse> participants = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            ParticipantStats stat = stats.get(i);
            participants.add(new ParticipantResultResponse(
                    i + 1,
                    stat.user().getId(),
                    stat.user().getNickname(),
                    stat.user().getProfileImageUrl(),
                    stat.totalDeviation(),
                    stat.missingPredictionCount(),
                    stat.totalDeviation() == bestDeviation,
                    loserIds.contains(stat.user().getId()),
                    stat.submittedCount()
            ));
        }
        List<ScorePrediction> allPredictions = scorePredictionRepository.findByRoomId(room.getId());
        List<PredictionRoomParticipant> allParticipants = participantRepository.findByRoomId(room.getId());
        List<MatchPredictionResultResponse> matchResults = roomMatches.stream().map(roomMatch -> {
            FootballMatch match = roomMatch.getFootballMatch();
            List<UserPredictionResultResponse> predictions = allParticipants.stream().map(participant -> {
                ScorePrediction prediction = allPredictions.stream()
                        .filter(p -> p.getUser().getId().equals(participant.getUser().getId()) && p.getFootballMatch().getId().equals(match.getId()))
                        .findFirst().orElse(null);
                if (prediction == null) {
                    return new UserPredictionResultResponse(participant.getUser().getId(), participant.getUser().getNickname(), null, null, deviationCalculator.missingPredictionPenalty(), true);
                }
                return new UserPredictionResultResponse(participant.getUser().getId(), participant.getUser().getNickname(), prediction.getPredictedHomeScore(), prediction.getPredictedAwayScore(), prediction.getDeviation(), false);
            }).toList();
            return new MatchPredictionResultResponse(match.getId(), match.getLeague().getName(), TeamResponse.from(match.getHomeTeam()), TeamResponse.from(match.getAwayTeam()), match.getHomeScore(), match.getAwayScore(), predictions);
        }).toList();
        ParticipantStats primaryLoser = losers.isEmpty() ? null : losers.get(0);
        return new PredictionRoomResultResponse(
                room.getId(),
                room.getTitle(),
                PredictionRoomStatus.COMPLETED,
                primaryLoser == null ? null : primaryLoser.user().getId(),
                primaryLoser == null ? null : "총 편차 " + primaryLoser.totalDeviation() + "점으로 가장 멀리 빗나갔어요.",
                participants,
                matchResults,
                "동률일 경우 누락 예측 수, 최대 단일 경기 편차, 마지막 제출 시간이 늦은 순서로 꼴찌를 선정합니다."
        );
    }

    public record ParticipantStats(User user, int totalDeviation, int missingPredictionCount, int maxSingleDeviation, LocalDateTime lastSubmittedAt, int submittedCount) {
    }

    private void notifyResult(PredictionRoom room, List<PredictionRoomParticipant> participants, List<ParticipantStats> losers) {
        Set<Long> loserIds = losers.stream().map(stat -> stat.user().getId()).collect(Collectors.toSet());
        for (PredictionRoomParticipant participant : participants) {
            boolean loser = loserIds.contains(participant.getUser().getId());
            notificationService.create(
                    participant.getUser(),
                    loser ? NotificationType.LOSER_SELECTED : NotificationType.MATCH_RESULT_FINALIZED,
                    loser ? "예측방 꼴찌가 선정됐어요." : "예측방 결과가 확정됐어요.",
                    loser ? "'" + room.getTitle() + "'에서 가장 크게 빗나갔어요." : "'" + room.getTitle() + "' 결과를 확인해보세요.",
                    "/rooms/" + room.getId() + "/result"
            );
        }
    }
}
