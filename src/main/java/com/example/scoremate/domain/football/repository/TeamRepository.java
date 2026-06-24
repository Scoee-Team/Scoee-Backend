package com.example.scoremate.domain.football.repository;

import com.example.scoremate.domain.football.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Page<Team> findByLeagueIdAndNameContainingIgnoreCase(Long leagueId, String query, Pageable pageable);
    Page<Team> findByLeagueId(Long leagueId, Pageable pageable);
    Page<Team> findByNameContainingIgnoreCase(String query, Pageable pageable);
    Optional<Team> findByExternalTeamId(Long externalTeamId);
}
