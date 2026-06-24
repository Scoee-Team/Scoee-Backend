package com.example.scoremate.domain.prediction.dto;

import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public final class PredictionDtos {
    private PredictionDtos() {
    }

    public record SubmitPredictionsRequest(
            @NotEmpty(message = "예측 목록은 비어 있을 수 없습니다.")
            List<@Valid ScorePredictionRequest> predictions
    ) {
    }

    public record ScorePredictionRequest(
            @NotNull(message = "matchId는 필수입니다.") Long matchId,
            @NotNull(message = "홈 점수는 필수입니다.") @Min(value = 0, message = "스코어는 0 이상이어야 합니다.") @Max(value = 20, message = "스코어는 20 이하여야 합니다.") Integer homeScore,
            @NotNull(message = "원정 점수는 필수입니다.") @Min(value = 0, message = "스코어는 0 이상이어야 합니다.") @Max(value = 20, message = "스코어는 20 이하여야 합니다.") Integer awayScore
    ) {
    }

    public record SubmitPredictionsResponse(Long roomId, long submittedCount, int totalCount, PredictionStatus status) {
    }

    public record CrowdSummaryResponse(int homeWinPercent, int drawPercent, int awayWinPercent) {
    }

    public record MyPredictionMatchResponse(
            Long matchId,
            String leagueName,
            TeamResponse homeTeam,
            TeamResponse awayTeam,
            LocalDateTime kickoffTime,
            LocalDateTime predictionDeadline,
            boolean locked,
            PredictionStatus predictionStatus,
            Integer homeScore,
            Integer awayScore,
            CrowdSummaryResponse crowdSummary
    ) {
    }

    public record MyPredictionsResponse(Long roomId, String roomTitle, PredictionRoomStatus status, List<MyPredictionMatchResponse> matches) {
    }

    public record ParticipantResultResponse(
            int rank,
            Long userId,
            String nickname,
            String profileImageUrl,
            int totalDeviation,
            int missingPredictionCount,
            boolean isBest,
            boolean isLoser,
            int submittedMatchCount
    ) {
    }

    public record UserPredictionResultResponse(
            Long userId,
            String nickname,
            Integer predictedHomeScore,
            Integer predictedAwayScore,
            Integer deviation,
            Boolean missingPrediction
    ) {
    }

    public record MatchPredictionResultResponse(
            Long matchId,
            String leagueName,
            TeamResponse homeTeam,
            TeamResponse awayTeam,
            Integer actualHomeScore,
            Integer actualAwayScore,
            List<UserPredictionResultResponse> predictions
    ) {
    }

    public record PredictionRoomResultResponse(
            Long roomId,
            String roomTitle,
            PredictionRoomStatus roomStatus,
            Long loserUserId,
            String loserMessage,
            List<ParticipantResultResponse> participants,
            List<MatchPredictionResultResponse> matchResults,
            String tiebreakerDescription
    ) {
    }
}
