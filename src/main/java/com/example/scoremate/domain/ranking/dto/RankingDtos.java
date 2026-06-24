package com.example.scoremate.domain.ranking.dto;

import java.util.List;

public final class RankingDtos {
    private RankingDtos() {
    }

    public enum RankingPeriod {
        ALL, MONTH, SEASON
    }

    public record RoomRankingRowResponse(
            int rank,
            Long userId,
            String nickname,
            String profileImageUrl,
            int totalDeviation,
            int matchCount,
            double averageDeviation,
            boolean isBest,
            boolean isLoser
    ) {
    }

    public record RoomRankingResponse(
            Long roomId,
            String roomTitle,
            int reflectedMatchCount,
            int bestDeviation,
            List<RoomRankingRowResponse> rows
    ) {
    }

    public record MyRankingResponse(
            String nickname,
            int overallRank,
            int monthlyRank,
            long activeRoomCount,
            double averageDeviation,
            long bestPredictionCount,
            long loserCount,
            long completedRoomCount
    ) {
    }
}
