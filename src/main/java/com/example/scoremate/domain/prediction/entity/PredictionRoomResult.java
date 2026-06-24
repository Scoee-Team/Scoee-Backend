package com.example.scoremate.domain.prediction.entity;

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
public class PredictionRoomResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private User loser;

    private Integer loserTotalDeviation;

    @Column(nullable = false)
    private LocalDateTime calculatedAt;
}
