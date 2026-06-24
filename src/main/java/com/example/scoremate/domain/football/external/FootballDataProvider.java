package com.example.scoremate.domain.football.external;

import java.time.LocalDate;
import java.util.List;

public interface FootballDataProvider {
    List<ExternalMatchResponse> getUpcomingMatches(LocalDate date);
    List<ExternalMatchResponse> getMatchesByCompetition(Long competitionId, Integer season);
    ExternalMatchResponse getMatchDetail(Long externalMatchId);
}
