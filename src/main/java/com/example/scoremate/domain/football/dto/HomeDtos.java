package com.example.scoremate.domain.football.dto;

import com.example.scoremate.domain.football.dto.FootballDtos.MatchSummaryResponse;
import com.example.scoremate.domain.predictionroom.dto.PredictionRoomDtos.PredictionRoomSummaryResponse;

import java.util.List;

public final class HomeDtos {
    private HomeDtos() {
    }

    public record HomeResponse(
            List<MatchSummaryResponse> todayMatches,
            List<MatchSummaryResponse> favoriteTeamMatches,
            List<PredictionRoomSummaryResponse> activeRooms,
            List<MatchSummaryResponse> recommendedMatches,
            long unreadNotificationCount
    ) {
    }
}
