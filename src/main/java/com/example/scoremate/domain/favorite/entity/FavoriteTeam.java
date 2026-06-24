package com.example.scoremate.domain.favorite.entity;

import com.example.scoremate.domain.football.entity.Team;
import com.example.scoremate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_favorite_team", columnNames = {"user_id", "team_id"}))
public class FavoriteTeam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team team;
}
