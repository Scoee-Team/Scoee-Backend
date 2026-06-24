package com.example.scoremate.domain.football.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_league_external", columnList = "externalLeagueId"))
public class League {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalLeagueId;
    private String name;
    private String country;
    private String logoUrl;
    private Integer season;
    private String round;
}
