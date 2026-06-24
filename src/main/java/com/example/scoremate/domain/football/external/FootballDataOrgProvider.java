package com.example.scoremate.domain.football.external;

import com.example.scoremate.domain.football.entity.MatchStatus;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "scoremate.football-data", name = "mock-enabled", havingValue = "false", matchIfMissing = true)
public class FootballDataOrgProvider implements FootballDataProvider {
    private final FootballDataOrgClientProperties properties;

    @Override
    public List<ExternalMatchResponse> getUpcomingMatches(LocalDate date) {
        FootballDataOrgMatchesResponse response = get("/matches?dateFrom={dateFrom}&dateTo={dateTo}", FootballDataOrgMatchesResponse.class, date, date.plusDays(1));
        return response.matches().stream().map(this::toExternalMatch).toList();
    }

    @Override
    public List<ExternalMatchResponse> getMatchesByCompetition(Long competitionId, Integer season) {
        FootballDataOrgMatchesResponse response = get("/competitions/{id}/matches?season={season}", FootballDataOrgMatchesResponse.class, competitionId, season);
        return response.matches().stream().map(this::toExternalMatch).toList();
    }

    @Override
    public ExternalMatchResponse getMatchDetail(Long externalMatchId) {
        FootballDataOrgMatch response = get("/matches/{id}", FootballDataOrgMatch.class, externalMatchId);
        return toExternalMatch(response);
    }

    private <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        if (properties.apiToken() == null || properties.apiToken().isBlank()) {
            throw new BusinessException(ErrorCode.EXTERNAL_FOOTBALL_API_ERROR);
        }
        try {
            return RestClient.builder()
                    .baseUrl(properties.resolvedBaseUrl())
                    .defaultHeader("X-Auth-Token", properties.apiToken())
                    .build()
                    .get()
                    .uri(uri, uriVariables)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_FOOTBALL_API_ERROR);
                    })
                    .body(responseType);
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_FOOTBALL_API_ERROR);
        }
    }

    private ExternalMatchResponse toExternalMatch(FootballDataOrgMatch match) {
        Integer season = null;
        if (match.season() != null && match.season().startDate() != null && match.season().startDate().length() >= 4) {
            season = Integer.parseInt(match.season().startDate().substring(0, 4));
        }
        Integer homeScore = null;
        Integer awayScore = null;
        if (match.score() != null && match.score().fullTime() != null) {
            homeScore = match.score().fullTime().home();
            awayScore = match.score().fullTime().away();
        }
        return new ExternalMatchResponse(
                match.id(),
                match.competition().id(),
                match.competition().name(),
                match.area() == null ? null : match.area().name(),
                match.competition().emblem(),
                season,
                match.homeTeam().id(),
                match.homeTeam().name(),
                match.homeTeam().crest(),
                match.awayTeam().id(),
                match.awayTeam().name(),
                match.awayTeam().crest(),
                OffsetDateTime.parse(match.utcDate()).toLocalDateTime(),
                mapStatus(match.status()),
                homeScore,
                awayScore,
                match.venue()
        );
    }

    private MatchStatus mapStatus(String externalStatus) {
        if (externalStatus == null) {
            return MatchStatus.SCHEDULED;
        }
        return switch (externalStatus) {
            case "FINISHED", "AWARDED" -> MatchStatus.FINISHED;
            case "IN_PLAY", "PAUSED", "EXTRA_TIME", "PENALTY_SHOOTOUT" -> MatchStatus.LIVE;
            case "POSTPONED", "SUSPENDED" -> MatchStatus.POSTPONED;
            case "CANCELLED" -> MatchStatus.CANCELLED;
            default -> MatchStatus.SCHEDULED;
        };
    }

    private record FootballDataOrgMatchesResponse(List<FootballDataOrgMatch> matches) {
    }

    private record FootballDataOrgMatch(
            Long id,
            FootballDataOrgArea area,
            FootballDataOrgCompetition competition,
            FootballDataOrgSeason season,
            String utcDate,
            String status,
            String venue,
            FootballDataOrgTeam homeTeam,
            FootballDataOrgTeam awayTeam,
            FootballDataOrgScore score
    ) {
    }

    private record FootballDataOrgArea(String name) {
    }

    private record FootballDataOrgCompetition(Long id, String name, String code, String emblem) {
    }

    private record FootballDataOrgSeason(String startDate) {
    }

    private record FootballDataOrgTeam(Long id, String name, String shortName, String tla, String crest) {
    }

    private record FootballDataOrgScore(FootballDataOrgScoreValue fullTime) {
    }

    private record FootballDataOrgScoreValue(Integer home, Integer away) {
    }
}
