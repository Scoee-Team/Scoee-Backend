package com.example.scoremate.domain.predictionroom.dto;

import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.MatchStatus;
import com.example.scoremate.domain.prediction.dto.PredictionStatus;
import com.example.scoremate.domain.predictionroom.entity.PredictionDeadlineType;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomStatus;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomType;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public final class PredictionRoomDtos {
    private PredictionRoomDtos() {
    }

    public enum PredictionRoomFilter {
        ALL, NEEDS_INPUT, COMPLETED
    }

    public record HostResponse(Long id, String nickname, String profileImageUrl) {
    }

    public record CreatePredictionRoomRequest(
            @NotBlank(message = "방 제목은 필수입니다.")
            @Size(max = 40, message = "방 제목은 40자 이하여야 합니다.")
            String title,
            @NotNull(message = "방 타입은 필수입니다.") PredictionRoomType type,
            @NotEmpty(message = "경기는 최소 1개 이상 선택해야 합니다.") List<Long> matchIds,
            @Min(value = 2, message = "정원은 최소 2명입니다.")
            @Max(value = 100, message = "정원은 최대 100명입니다.")
            Integer capacity,
            String visibility,
            @NotNull(message = "예측 마감 타입은 필수입니다.") PredictionDeadlineType predictionDeadlineType,
            LocalDateTime sameDeadline
    ) {
    }

    public record CreatePredictionRoomResponse(Long id) {
    }

    public record JoinPredictionRoomRequest(@NotBlank(message = "초대 코드는 필수입니다.") String inviteCode) {
    }

    public record InviteLinkResponse(Long roomId, String inviteCode, String inviteLink) {
    }

    public record PredictionRoomSummaryResponse(
            Long id,
            String title,
            PredictionRoomType type,
            HostResponse host,
            int matchCount,
            long participantCount,
            int capacity,
            PredictionRoomStatus status,
            PredictionStatus myPredictionStatus,
            String myPredictionLabel,
            LocalDateTime nextDeadline
    ) {
    }

    public record PredictionSummaryResponse(int submittedCount, int totalCount, PredictionStatus status) {
    }

    public record ParticipantResponse(Long userId, String nickname, String profileImageUrl, int submittedCount, int totalCount) {
    }

    public record RoomMatchResponse(
            Long matchId,
            String leagueName,
            TeamResponse homeTeam,
            TeamResponse awayTeam,
            LocalDateTime kickoffTime,
            LocalDateTime predictionDeadline,
            MatchStatus matchStatus,
            PredictionStatus predictionStatus,
            ScoreValueResponse myPrediction,
            boolean locked
    ) {
    }

    public record ScoreValueResponse(Integer homeScore, Integer awayScore) {
    }

    public record PredictionRoomDetailResponse(
            Long id,
            String title,
            PredictionRoomType type,
            PredictionRoomStatus status,
            HostResponse host,
            String inviteCode,
            String inviteLink,
            long participantCount,
            int capacity,
            PredictionSummaryResponse myPredictionSummary,
            LocalDateTime nextDeadline,
            List<ParticipantResponse> participants,
            List<RoomMatchResponse> matches
    ) {
    }
}
