package com.example.scoremate.domain.ranking.service;

import com.example.scoremate.domain.prediction.entity.PredictionRoomResult;
import com.example.scoremate.domain.prediction.entity.ScorePrediction;
import com.example.scoremate.domain.prediction.repository.PredictionRoomResultRepository;
import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.prediction.service.PredictionResultService;
import com.example.scoremate.domain.prediction.service.PredictionResultService.ParticipantStats;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoom;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomMatchRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomParticipantRepository;
import com.example.scoremate.domain.predictionroom.service.PredictionRoomService;
import com.example.scoremate.domain.ranking.dto.RankingDtos.*;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final CurrentUserProvider currentUserProvider;
    private final PredictionRoomService roomService;
    private final PredictionRoomMatchRepository roomMatchRepository;
    private final PredictionRoomParticipantRepository participantRepository;
    private final ScorePredictionRepository scorePredictionRepository;
    private final PredictionRoomResultRepository resultRepository;
    private final PredictionResultService resultService;

    @Transactional(readOnly = true)
    public RoomRankingResponse roomRanking(Long roomId) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomService.getRoom(roomId);
        roomService.ensureParticipant(roomId, user.getId());
        var matches = roomMatchRepository.findByRoomId(roomId);
        if (matches.stream().anyMatch(m -> !m.getFootballMatch().isFinished())) {
            throw new BusinessException(ErrorCode.RESULT_NOT_READY);
        }
        List<ParticipantStats> stats = resultService.calculateStats(room, matches);
        List<ParticipantStats> losers = resultService.selectLosers(stats);
        List<Long> loserIds = losers.stream().map(s -> s.user().getId()).toList();
        int best = stats.stream().mapToInt(ParticipantStats::totalDeviation).min().orElse(0);
        List<RoomRankingRowResponse> rows = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            ParticipantStats stat = stats.get(i);
            rows.add(new RoomRankingRowResponse(i + 1, stat.user().getId(), stat.user().getNickname(), stat.user().getProfileImageUrl(), stat.totalDeviation(), matches.size(), matches.isEmpty() ? 0 : (double) stat.totalDeviation() / matches.size(), stat.totalDeviation() == best, loserIds.contains(stat.user().getId())));
        }
        return new RoomRankingResponse(room.getId(), room.getTitle(), matches.size(), best, rows);
    }

    @Transactional(readOnly = true)
    public MyRankingResponse me(RankingPeriod period) {
        User user = currentUserProvider.getCurrentUser();
        List<ScorePrediction> predictions = scorePredictionRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(user.getId()) && p.getDeviation() != null)
                .toList();
        double avg = predictions.stream().mapToInt(ScorePrediction::getDeviation).average().orElse(0);
        long activeRooms = participantRepository.findByUserId(user.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(p -> p.getRoom().getStatus() != PredictionRoomStatus.COMPLETED).count();
        long completedRooms = participantRepository.findByUserId(user.getId(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(p -> p.getRoom().getStatus() == PredictionRoomStatus.COMPLETED).count();
        long loserCount = resultRepository.findAll().stream().filter(r -> r.getLoser() != null && r.getLoser().getId().equals(user.getId())).count();
        long bestCount = predictions.stream().filter(p -> p.getDeviation() != null && p.getDeviation() == 0).count();
        return new MyRankingResponse(user.getNickname(), 1, 1, activeRooms, avg, bestCount, loserCount, completedRooms);
    }
}
