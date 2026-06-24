package com.example.scoremate.domain.football.external;

import com.example.scoremate.domain.football.entity.MatchStatus;

import java.time.LocalDateTime;

public record ExternalMatchResponse(
        Long externalMatchId,
        Long externalLeagueId,
        String leagueName,
        String country,
        String logoUrl,
        Integer season,
        Long externalHomeTeamId,
        String homeTeamName,
        String homeTeamLogoUrl,
        Long externalAwayTeamId,
        String awayTeamName,
        String awayTeamLogoUrl,
        LocalDateTime kickoffTime,
        MatchStatus status,
        Integer homeScore,
        Integer awayScore,
        String venue
) {
}
