package com.example.scoremate.domain.football.repository;

import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.football.entity.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FootballMatchRepository extends JpaRepository<FootballMatch, Long> {
    Page<FootballMatch> findByKickoffTimeBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);
    Page<FootballMatch> findByKickoffTimeBetweenAndLeagueId(LocalDateTime from, LocalDateTime to, Long leagueId, Pageable pageable);
    List<FootballMatch> findByKickoffTimeBetween(LocalDateTime from, LocalDateTime to);
    Optional<FootballMatch> findByExternalMatchId(Long externalMatchId);

    @Query("""
            select m from FootballMatch m
            where (:from is null or m.kickoffTime >= :from)
              and (:to is null or m.kickoffTime <= :to)
              and (:leagueId is null or m.league.id = :leagueId)
              and (:status is null or m.status = :status)
            """)
    Page<FootballMatch> search(@Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to,
                               @Param("leagueId") Long leagueId,
                               @Param("status") MatchStatus status,
                               Pageable pageable);
}
