package com.example.scoremate.domain.football.repository;

import com.example.scoremate.domain.football.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeagueRepository extends JpaRepository<League, Long> {
    List<League> findBySeason(Integer season);
    Optional<League> findByExternalLeagueIdAndSeason(Long externalLeagueId, Integer season);
}
