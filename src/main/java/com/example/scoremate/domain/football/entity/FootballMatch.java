package com.example.scoremate.domain.football.entity;

import com.example.scoremate.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = {
        @Index(name = "idx_match_external", columnList = "externalMatchId"),
        @Index(name = "idx_match_kickoff", columnList = "kickoffTime")
})
public class FootballMatch extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalMatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team awayTeam;

    @Column(nullable = false)
    private LocalDateTime kickoffTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status;

    private Integer homeScore;
    private Integer awayScore;
    private String venue;

    public boolean isFinished() {
        return status == MatchStatus.FINISHED && homeScore != null && awayScore != null;
    }
}
