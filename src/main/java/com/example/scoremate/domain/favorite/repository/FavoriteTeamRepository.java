package com.example.scoremate.domain.favorite.repository;

import com.example.scoremate.domain.favorite.entity.FavoriteTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteTeamRepository extends JpaRepository<FavoriteTeam, Long> {
    List<FavoriteTeam> findByUserId(Long userId);
    Optional<FavoriteTeam> findByUserIdAndTeamId(Long userId, Long teamId);
    void deleteByUserIdAndTeamId(Long userId, Long teamId);
    boolean existsByUserIdAndTeamId(Long userId, Long teamId);
}
