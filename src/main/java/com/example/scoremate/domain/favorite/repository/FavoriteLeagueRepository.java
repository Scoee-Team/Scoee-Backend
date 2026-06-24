package com.example.scoremate.domain.favorite.repository;

import com.example.scoremate.domain.favorite.entity.FavoriteLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FavoriteLeagueRepository extends JpaRepository<FavoriteLeague, Long> {
    List<FavoriteLeague> findByUserId(Long userId);
    Optional<FavoriteLeague> findByUserIdAndLeagueId(Long userId, Long leagueId);
    void deleteByUserIdAndLeagueId(Long userId, Long leagueId);
    boolean existsByUserIdAndLeagueId(Long userId, Long leagueId);
    Set<FavoriteLeague> findAllByUserId(Long userId);
}
