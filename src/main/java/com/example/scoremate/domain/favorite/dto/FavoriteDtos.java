package com.example.scoremate.domain.favorite.dto;

import com.example.scoremate.domain.football.dto.FootballDtos.LeagueResponse;
import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;

import java.util.List;

public final class FavoriteDtos {
    private FavoriteDtos() {
    }

    public record FavoriteResponse(List<LeagueResponse> leagues, List<TeamResponse> teams) {
    }
}
