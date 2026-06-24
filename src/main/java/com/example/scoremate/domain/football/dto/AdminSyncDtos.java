package com.example.scoremate.domain.football.dto;

import com.example.scoremate.domain.football.entity.MatchStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AdminSyncDtos {
    private AdminSyncDtos() {
    }

    public record SyncResultResponse(String message, int affectedCount) {
    }

    public record UpsertMatchRequest(
            Long leagueId,
            Long homeTeamId,
            Long awayTeamId,
            LocalDateTime kickoffTime,
            MatchStatus status,
            Integer homeScore,
            Integer awayScore,
            String venue
    ) {
    }

    public record UpdateMatchResultRequest(
            @NotNull(message = "status는 필수입니다.") MatchStatus status,
            Integer homeScore,
            Integer awayScore
    ) {
    }

    public record SyncDateRequest(LocalDate date) {
    }
}
