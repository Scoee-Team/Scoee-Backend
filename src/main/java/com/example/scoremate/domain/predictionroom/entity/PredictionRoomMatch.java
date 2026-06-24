package com.example.scoremate.domain.predictionroom.entity;

import com.example.scoremate.domain.football.entity.FootballMatch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_room_match_room", columnList = "room_id"))
public class PredictionRoomMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatch footballMatch;

    @Column(nullable = false)
    private LocalDateTime predictionDeadline;

    public boolean isLocked(LocalDateTime now) {
        return !predictionDeadline.isAfter(now);
    }
}
