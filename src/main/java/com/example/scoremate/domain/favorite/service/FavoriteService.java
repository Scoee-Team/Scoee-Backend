package com.example.scoremate.domain.favorite.service;

import com.example.scoremate.domain.favorite.dto.FavoriteDtos.FavoriteResponse;
import com.example.scoremate.domain.favorite.entity.FavoriteLeague;
import com.example.scoremate.domain.favorite.entity.FavoriteTeam;
import com.example.scoremate.domain.favorite.repository.FavoriteLeagueRepository;
import com.example.scoremate.domain.favorite.repository.FavoriteTeamRepository;
import com.example.scoremate.domain.football.dto.FootballDtos.LeagueResponse;
import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.League;
import com.example.scoremate.domain.football.entity.Team;
import com.example.scoremate.domain.football.repository.LeagueRepository;
import com.example.scoremate.domain.football.repository.TeamRepository;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final CurrentUserProvider currentUserProvider;
    private final FavoriteLeagueRepository favoriteLeagueRepository;
    private final FavoriteTeamRepository favoriteTeamRepository;
    private final LeagueRepository leagueRepository;
    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public FavoriteResponse getFavorites() {
        User user = currentUserProvider.getCurrentUser();
        return new FavoriteResponse(
                favoriteLeagueRepository.findByUserId(user.getId()).stream().map(f -> LeagueResponse.from(f.getLeague())).toList(),
                favoriteTeamRepository.findByUserId(user.getId()).stream().map(f -> TeamResponse.from(f.getTeam())).toList()
        );
    }

    @Transactional
    public void addLeague(Long leagueId) {
        User user = currentUserProvider.getCurrentUser();
        League league = leagueRepository.findById(leagueId).orElseThrow(() -> new BusinessException(ErrorCode.LEAGUE_NOT_FOUND));
        if (!favoriteLeagueRepository.existsByUserIdAndLeagueId(user.getId(), leagueId)) {
            favoriteLeagueRepository.save(FavoriteLeague.builder().user(user).league(league).build());
        }
    }

    @Transactional
    public void removeLeague(Long leagueId) {
        User user = currentUserProvider.getCurrentUser();
        favoriteLeagueRepository.deleteByUserIdAndLeagueId(user.getId(), leagueId);
    }

    @Transactional
    public void addTeam(Long teamId) {
        User user = currentUserProvider.getCurrentUser();
        Team team = teamRepository.findById(teamId).orElseThrow(() -> new BusinessException(ErrorCode.TEAM_NOT_FOUND));
        if (!favoriteTeamRepository.existsByUserIdAndTeamId(user.getId(), teamId)) {
            favoriteTeamRepository.save(FavoriteTeam.builder().user(user).team(team).build());
        }
    }

    @Transactional
    public void removeTeam(Long teamId) {
        User user = currentUserProvider.getCurrentUser();
        favoriteTeamRepository.deleteByUserIdAndTeamId(user.getId(), teamId);
    }
}
