package com.example.scoremate.domain.football.controller;

import com.example.scoremate.domain.football.dto.AdminSyncDtos.SyncResultResponse;
import com.example.scoremate.domain.football.dto.AdminSyncDtos.UpdateMatchResultRequest;
import com.example.scoremate.domain.football.dto.FootballDtos.*;
import com.example.scoremate.domain.football.dto.HomeDtos.HomeResponse;
import com.example.scoremate.domain.football.entity.MatchStatus;
import com.example.scoremate.domain.football.service.FootballMatchService;
import com.example.scoremate.domain.football.service.HomeService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.response.PageResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class FootballController {
    private final FootballMatchService footballMatchService;
    private final HomeService homeService;

    @Tag(name = "Home")
    @Operation(summary = "홈 요약", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping("/home")
    public ApiResponse<HomeResponse> home() {
        return ApiResponse.success(homeService.home());
    }

    @Tag(name = "Matches")
    @Operation(summary = "경기 목록 조회", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping("/matches")
    public ApiResponse<PageResponse<MatchSummaryResponse>> matches(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly,
            @RequestParam(required = false) MatchStatus status,
            Pageable pageable
    ) {
        return ApiResponse.success(footballMatchService.search(date, from, to, leagueId, favoriteOnly, status, pageable));
    }

    @Tag(name = "Matches")
    @Operation(summary = "오늘 경기 조회", description = "MATCH_NOT_FOUND")
    @GetMapping("/matches/today")
    public ApiResponse<PageResponse<MatchSummaryResponse>> today(Pageable pageable) {
        return ApiResponse.success(footballMatchService.search(LocalDate.now(), null, null, null, false, null, pageable));
    }

    @Tag(name = "Matches")
    @Operation(summary = "다가오는 경기 조회", description = "date 기준 경기 목록을 반환합니다.")
    @GetMapping("/matches/upcoming")
    public ApiResponse<PageResponse<MatchSummaryResponse>> upcoming(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long leagueId,
            Pageable pageable
    ) {
        return ApiResponse.success(footballMatchService.search(date == null ? LocalDate.now() : date, null, null, leagueId, false, null, pageable));
    }

    @Tag(name = "Matches")
    @Operation(summary = "날짜별 경기 수", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping("/matches/calendar")
    public ApiResponse<List<CalendarMatchCountResponse>> calendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long leagueId,
            @RequestParam(defaultValue = "false") Boolean favoriteOnly
    ) {
        return ApiResponse.success(footballMatchService.calendar(from, to, leagueId, favoriteOnly));
    }

    @Tag(name = "Matches")
    @Operation(summary = "경기 상세", description = "MATCH_NOT_FOUND")
    @GetMapping("/matches/{matchId}")
    public ApiResponse<MatchSummaryResponse> match(@PathVariable Long matchId) {
        return ApiResponse.success(footballMatchService.detail(matchId));
    }

    @Tag(name = "Matches")
    @Operation(summary = "대회 경기 조회", description = "competitionId는 현재 MVP에서 leagueId와 동일하게 처리합니다.")
    @GetMapping("/competitions/{competitionId}/matches")
    public ApiResponse<PageResponse<MatchSummaryResponse>> competitionMatches(@PathVariable Long competitionId, @RequestParam(required = false) Integer season, Pageable pageable) {
        return ApiResponse.success(footballMatchService.search(null, null, null, competitionId, false, null, pageable));
    }

    @Tag(name = "Leagues")
    @Operation(summary = "리그 목록")
    @GetMapping("/leagues")
    public ApiResponse<List<LeagueResponse>> leagues(@RequestParam(required = false) Integer season) {
        return ApiResponse.success(footballMatchService.leagues(season));
    }

    @Tag(name = "Teams")
    @Operation(summary = "팀 목록/검색")
    @GetMapping("/teams")
    public ApiResponse<PageResponse<TeamResponse>> teams(@RequestParam(required = false) Long leagueId, @RequestParam(required = false) String query, Pageable pageable) {
        return ApiResponse.success(footballMatchService.teams(leagueId, query, pageable));
    }

    @Tag(name = "Admin")
    @Operation(summary = "경기 데이터 동기화", description = "관리자/내부 API. EXTERNAL_FOOTBALL_API_ERROR")
    @PostMapping("/admin/sync/matches")
    public ApiResponse<SyncResultResponse> syncMatches(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(footballMatchService.syncUpcoming(date));
    }

    @Tag(name = "Admin")
    @Operation(summary = "경기 데이터 동기화 alias", description = "AGENTS.md 권장 경로 호환 endpoint입니다.")
    @PostMapping("/admin/matches/sync")
    public ApiResponse<SyncResultResponse> syncMatchesAlias(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(footballMatchService.syncUpcoming(date));
    }

    @Tag(name = "Admin")
    @Operation(summary = "대회 경기 데이터 동기화", description = "football-data.org /v4/competitions/{id}/matches 기반입니다. 예: WC=2000, PL=2021")
    @PostMapping("/admin/sync/competitions/{competitionId}/matches")
    public ApiResponse<SyncResultResponse> syncCompetitionMatches(@PathVariable Long competitionId, @RequestParam(required = false) Integer season) {
        return ApiResponse.success(footballMatchService.syncCompetition(competitionId, season));
    }

    @Tag(name = "Admin")
    @Operation(summary = "외부 경기 상세 동기화", description = "football-data.org external match id를 기준으로 /v4/matches/{id}를 호출합니다.")
    @PostMapping("/admin/sync/external-matches/{externalMatchId}")
    public ApiResponse<SyncResultResponse> syncExternalMatch(@PathVariable Long externalMatchId) {
        return ApiResponse.success(footballMatchService.syncMatchDetail(externalMatchId));
    }

    @Tag(name = "Admin")
    @Operation(summary = "리그 데이터 동기화", description = "현재 MVP에서는 경기 sync 시 리그를 함께 정규화합니다.")
    @PostMapping("/admin/sync/leagues")
    public ApiResponse<SyncResultResponse> syncLeagues(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(footballMatchService.syncUpcoming(date));
    }

    @Tag(name = "Admin")
    @Operation(summary = "팀 데이터 동기화", description = "현재 MVP에서는 경기 sync 시 팀을 함께 정규화합니다.")
    @PostMapping("/admin/sync/teams")
    public ApiResponse<SyncResultResponse> syncTeams(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(footballMatchService.syncUpcoming(date));
    }

    @Tag(name = "Admin")
    @Operation(summary = "결과 동기화", description = "외부 provider 확장 전까지는 수동 match result update 후 calculate-result를 호출합니다.")
    @PostMapping("/admin/sync/results")
    public ApiResponse<SyncResultResponse> syncResults(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(new SyncResultResponse("결과 동기화 endpoint 준비 완료", 0));
    }

    @Tag(name = "Admin")
    @Operation(summary = "경기 결과 수동 업데이트", description = "MATCH_NOT_FOUND")
    @PostMapping("/admin/matches/{matchId}/sync")
    public ApiResponse<SyncResultResponse> updateMatchResult(@PathVariable Long matchId, @Valid @RequestBody UpdateMatchResultRequest request) {
        return ApiResponse.success(footballMatchService.updateResult(matchId, request));
    }
}
