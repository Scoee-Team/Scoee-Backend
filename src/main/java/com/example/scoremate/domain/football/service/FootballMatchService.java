package com.example.scoremate.domain.football.service;

import com.example.scoremate.domain.favorite.repository.FavoriteLeagueRepository;
import com.example.scoremate.domain.favorite.repository.FavoriteTeamRepository;
import com.example.scoremate.domain.football.dto.AdminSyncDtos.SyncResultResponse;
import com.example.scoremate.domain.football.dto.AdminSyncDtos.UpdateMatchResultRequest;
import com.example.scoremate.domain.football.dto.FootballDtos.CalendarMatchCountResponse;
import com.example.scoremate.domain.football.dto.FootballDtos.LeagueResponse;
import com.example.scoremate.domain.football.dto.FootballDtos.MatchSummaryResponse;
import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.*;
import com.example.scoremate.domain.football.external.ExternalMatchResponse;
import com.example.scoremate.domain.football.external.FootballDataProvider;
import com.example.scoremate.domain.football.repository.FootballMatchRepository;
import com.example.scoremate.domain.football.repository.LeagueRepository;
import com.example.scoremate.domain.football.repository.TeamRepository;
import com.example.scoremate.domain.prediction.dto.PredictionStatus;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.response.PageResponse;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FootballMatchService {
    private final FootballMatchRepository matchRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;
    private final FavoriteLeagueRepository favoriteLeagueRepository;
    private final FavoriteTeamRepository favoriteTeamRepository;
    private final FootballDataProvider footballDataProvider;
    private final CurrentUserProvider currentUserProvider;

    @Transactional(readOnly = true)
    public PageResponse<MatchSummaryResponse> search(LocalDate date, LocalDateTime from, LocalDateTime to, Long leagueId, Boolean favoriteOnly, MatchStatus status, Pageable pageable) {
        if (date != null) {
            from = date.atStartOfDay();
            to = date.plusDays(1).atStartOfDay().minusNanos(1);
        }
        Page<FootballMatch> page = matchRepository.search(from, to, leagueId, status, pageable);
        List<MatchSummaryResponse> responses = page.getContent().stream()
                .filter(match -> !Boolean.TRUE.equals(favoriteOnly) || isFavoriteMatch(match))
                .map(match -> MatchSummaryResponse.from(match, PredictionStatus.NOT_SUBMITTED))
                .toList();
        return new PageResponse<>(responses, new PageResponse.PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public List<CalendarMatchCountResponse> calendar(LocalDate from, LocalDate to, Long leagueId, Boolean favoriteOnly) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay().minusNanos(1);
        Map<LocalDate, List<FootballMatch>> byDate = matchRepository.findByKickoffTimeBetween(start, end).stream()
                .filter(match -> leagueId == null || match.getLeague().getId().equals(leagueId))
                .filter(match -> !Boolean.TRUE.equals(favoriteOnly) || isFavoriteMatch(match))
                .collect(Collectors.groupingBy(match -> match.getKickoffTime().toLocalDate()));
        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new CalendarMatchCountResponse(entry.getKey(), entry.getValue().size(), entry.getValue().stream().filter(this::isFavoriteMatch).count()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MatchSummaryResponse detail(Long matchId) {
        FootballMatch match = getMatch(matchId);
        return MatchSummaryResponse.from(match, PredictionStatus.NOT_SUBMITTED);
    }

    @Transactional(readOnly = true)
    public List<LeagueResponse> leagues(Integer season) {
        List<League> leagues = season == null ? leagueRepository.findAll() : leagueRepository.findBySeason(season);
        return leagues.stream().map(LeagueResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> teams(Long leagueId, String query, Pageable pageable) {
        Page<Team> page;
        if (leagueId != null && query != null && !query.isBlank()) {
            page = teamRepository.findByLeagueIdAndNameContainingIgnoreCase(leagueId, query, pageable);
        } else if (leagueId != null) {
            page = teamRepository.findByLeagueId(leagueId, pageable);
        } else if (query != null && !query.isBlank()) {
            page = teamRepository.findByNameContainingIgnoreCase(query, pageable);
        } else {
            page = teamRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(TeamResponse::from));
    }

    @Transactional
    public SyncResultResponse syncUpcoming(LocalDate date) {
        List<ExternalMatchResponse> externalMatches = footballDataProvider.getUpcomingMatches(date == null ? LocalDate.now() : date);
        int count = 0;
        for (ExternalMatchResponse external : externalMatches) {
            upsertExternal(external);
            count++;
        }
        return new SyncResultResponse("경기 데이터 동기화 완료", count);
    }

    @Transactional
    public SyncResultResponse syncCompetition(Long competitionId, Integer season) {
        List<ExternalMatchResponse> externalMatches = footballDataProvider.getMatchesByCompetition(competitionId, season == null ? LocalDate.now().getYear() : season);
        int count = 0;
        for (ExternalMatchResponse external : externalMatches) {
            upsertExternal(external);
            count++;
        }
        return new SyncResultResponse("대회 경기 데이터 동기화 완료", count);
    }

    @Transactional
    public SyncResultResponse syncMatchDetail(Long externalMatchId) {
        upsertExternal(footballDataProvider.getMatchDetail(externalMatchId));
        return new SyncResultResponse("경기 상세 동기화 완료", 1);
    }

    @Transactional
    public SyncResultResponse updateResult(Long matchId, UpdateMatchResultRequest request) {
        FootballMatch match = getMatch(matchId);
        match.setStatus(request.status());
        match.setHomeScore(request.homeScore());
        match.setAwayScore(request.awayScore());
        return new SyncResultResponse("경기 결과 업데이트 완료", 1);
    }

    public FootballMatch getMatch(Long matchId) {
        return matchRepository.findById(matchId).orElseThrow(() -> new BusinessException(ErrorCode.MATCH_NOT_FOUND));
    }

    private void upsertExternal(ExternalMatchResponse external) {
        League league = leagueRepository.findByExternalLeagueIdAndSeason(external.externalLeagueId(), external.season())
                .orElseGet(() -> leagueRepository.save(League.builder()
                        .externalLeagueId(external.externalLeagueId())
                        .name(external.leagueName())
                        .country(external.country())
                        .logoUrl(external.logoUrl())
                        .season(external.season())
                        .build()));
        league.setName(external.leagueName());
        league.setCountry(external.country());
        league.setLogoUrl(external.logoUrl());
        Team home = findOrCreateTeam(league, external.externalHomeTeamId(), external.homeTeamName());
        Team away = findOrCreateTeam(league, external.externalAwayTeamId(), external.awayTeamName());
        home.setLogoUrl(external.homeTeamLogoUrl());
        away.setLogoUrl(external.awayTeamLogoUrl());
        FootballMatch match = matchRepository.findByExternalMatchId(external.externalMatchId())
                .orElseGet(() -> FootballMatch.builder().externalMatchId(external.externalMatchId()).build());
        match.setLeague(league);
        match.setHomeTeam(home);
        match.setAwayTeam(away);
        match.setKickoffTime(external.kickoffTime());
        match.setStatus(external.status());
        match.setHomeScore(external.homeScore());
        match.setAwayScore(external.awayScore());
        match.setVenue(external.venue());
        matchRepository.save(match);
    }

    private Team findOrCreateTeam(League league, Long externalTeamId, String name) {
        Team team = teamRepository.findByExternalTeamId(externalTeamId)
                .orElseGet(() -> teamRepository.save(Team.builder()
                        .externalTeamId(externalTeamId)
                        .league(league)
                        .name(name)
                        .shortName(name)
                        .build()));
        team.setLeague(league);
        team.setName(name);
        team.setShortName(name);
        return team;
    }

    private boolean isFavoriteMatch(FootballMatch match) {
        try {
            Long userId = currentUserProvider.resolveUserId();
            return favoriteLeagueRepository.existsByUserIdAndLeagueId(userId, match.getLeague().getId())
                    || favoriteTeamRepository.existsByUserIdAndTeamId(userId, match.getHomeTeam().getId())
                    || favoriteTeamRepository.existsByUserIdAndTeamId(userId, match.getAwayTeam().getId());
        } catch (BusinessException e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> todayTop(int limit) {
        LocalDate today = LocalDate.now();
        return matchRepository.findByKickoffTimeBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()).stream()
                .sorted(Comparator.comparing(FootballMatch::getKickoffTime))
                .limit(limit)
                .map(match -> MatchSummaryResponse.from(match, PredictionStatus.NOT_SUBMITTED))
                .toList();
    }
}
