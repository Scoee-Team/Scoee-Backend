package com.example.scoremate.domain.prediction.entity;

import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoom;
import com.example.scoremate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_prediction_room_user_match",
        columnNames = {"room_id", "user_id", "football_match_id"}
), indexes = {
        @Index(name = "idx_prediction_room", columnList = "room_id"),
        @Index(name = "idx_prediction_user", columnList = "user_id"),
        @Index(name = "idx_prediction_match", columnList = "football_match_id")
})
public class ScorePrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatch footballMatch;

    @Column(nullable = false)
    private Integer predictedHomeScore;

    @Column(nullable = false)
    private Integer predictedAwayScore;

    private Integer deviation;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
