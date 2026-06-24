package com.example.scoremate.domain.football.external;

import com.example.scoremate.domain.football.entity.MatchStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "scoremate.football-data", name = "mock-enabled", havingValue = "true")
public class MockFootballDataProvider implements FootballDataProvider {
    @Override
    public List<ExternalMatchResponse> getUpcomingMatches(LocalDate date) {
        return List.of(
                new ExternalMatchResponse(100001L, 39L, "Premier League", "England", null, date.getYear(), 1L, "Manchester City", null, 2L, "Arsenal", null, date.atTime(20, 0), MatchStatus.SCHEDULED, null, null, "Etihad Stadium"),
                new ExternalMatchResponse(100002L, 39L, "Premier League", "England", null, date.getYear(), 3L, "Liverpool", null, 4L, "Chelsea", null, date.atTime(22, 0), MatchStatus.SCHEDULED, null, null, "Anfield")
        );
    }

    @Override
    public List<ExternalMatchResponse> getMatchesByCompetition(Long competitionId, Integer season) {
        return getUpcomingMatches(LocalDate.of(season, 6, 15));
    }

    @Override
    public ExternalMatchResponse getMatchDetail(Long externalMatchId) {
        return getUpcomingMatches(LocalDate.now()).stream()
                .filter(match -> match.externalMatchId().equals(externalMatchId))
                .findFirst()
                .orElseThrow();
    }
}
