package com.example.scoremate.domain.favorite.entity;

import com.example.scoremate.domain.football.entity.League;
import com.example.scoremate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_favorite_league", columnNames = {"user_id", "league_id"}))
public class FavoriteLeague {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private League league;
}
