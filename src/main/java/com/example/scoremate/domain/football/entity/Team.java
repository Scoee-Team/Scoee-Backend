package com.example.scoremate.domain.football.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_team_external", columnList = "externalTeamId"))
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalTeamId;
    private String name;
    private String shortName;
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    private League league;
}
