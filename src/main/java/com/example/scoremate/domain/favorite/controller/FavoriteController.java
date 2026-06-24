package com.example.scoremate.domain.favorite.controller;

import com.example.scoremate.domain.favorite.dto.FavoriteDtos.FavoriteResponse;
import com.example.scoremate.domain.favorite.service.FavoriteService;
import com.example.scoremate.global.response.ApiResponse;
import com.example.scoremate.global.swagger.SwaggerErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Favorites")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/favorites")
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(summary = "내 관심 목록", description = SwaggerErrorExamples.AUTH_AND_DOMAIN)
    @GetMapping
    public ApiResponse<FavoriteResponse> favorites() {
        return ApiResponse.success(favoriteService.getFavorites());
    }

    @Operation(summary = "관심 리그 추가", description = "LEAGUE_NOT_FOUND")
    @PostMapping("/leagues/{leagueId}")
    public ApiResponse<Void> addLeague(@PathVariable Long leagueId) {
        favoriteService.addLeague(leagueId);
        return ApiResponse.success();
    }

    @Operation(summary = "관심 리그 삭제")
    @DeleteMapping("/leagues/{leagueId}")
    public ApiResponse<Void> removeLeague(@PathVariable Long leagueId) {
        favoriteService.removeLeague(leagueId);
        return ApiResponse.success();
    }

    @Operation(summary = "관심 팀 추가", description = "TEAM_NOT_FOUND")
    @PostMapping("/teams/{teamId}")
    public ApiResponse<Void> addTeam(@PathVariable Long teamId) {
        favoriteService.addTeam(teamId);
        return ApiResponse.success();
    }

    @Operation(summary = "관심 팀 삭제")
    @DeleteMapping("/teams/{teamId}")
    public ApiResponse<Void> removeTeam(@PathVariable Long teamId) {
        favoriteService.removeTeam(teamId);
        return ApiResponse.success();
    }
}
