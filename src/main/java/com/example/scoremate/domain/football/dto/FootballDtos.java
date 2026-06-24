package com.example.scoremate.domain.football.dto;

import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.football.entity.League;
import com.example.scoremate.domain.football.entity.MatchStatus;
import com.example.scoremate.domain.football.entity.Team;
import com.example.scoremate.domain.prediction.dto.PredictionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class FootballDtos {
    private FootballDtos() {
    }

    public record LeagueResponse(Long id, String name, String country, String logoUrl, Integer season, String round) {
        public static LeagueResponse from(League league) {
            return new LeagueResponse(league.getId(), league.getName(), league.getCountry(), league.getLogoUrl(), league.getSeason(), league.getRound());
        }
    }

    public record TeamResponse(Long id, String name, String shortName, String logoUrl) {
        public static TeamResponse from(Team team) {
            return new TeamResponse(team.getId(), team.getName(), team.getShortName(), team.getLogoUrl());
        }
    }

    public record MatchSummaryResponse(
            Long id,
            LeagueResponse league,
            TeamResponse homeTeam,
            TeamResponse awayTeam,
            LocalDateTime kickoffTime,
            MatchStatus status,
            Integer homeScore,
            Integer awayScore,
            String venue,
            LocalDateTime predictionDeadline,
            PredictionStatus myPredictionStatus
    ) {
        public static MatchSummaryResponse from(FootballMatch match, PredictionStatus predictionStatus) {
            return new MatchSummaryResponse(
                    match.getId(),
                    LeagueResponse.from(match.getLeague()),
                    TeamResponse.from(match.getHomeTeam()),
                    TeamResponse.from(match.getAwayTeam()),
                    match.getKickoffTime(),
                    match.getStatus(),
                    match.getHomeScore(),
                    match.getAwayScore(),
                    match.getVenue(),
                    match.getKickoffTime(),
                    predictionStatus
            );
        }
    }

    public record CalendarMatchCountResponse(LocalDate date, long matchCount, long favoriteMatchCount) {
    }
}
