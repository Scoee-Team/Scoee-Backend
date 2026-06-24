package com.example.scoremate.domain.predictionroom.entity;

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
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_room_participant", columnNames = {"room_id", "user_id"}),
        indexes = {
                @Index(name = "idx_participant_room", columnList = "room_id"),
                @Index(name = "idx_participant_user", columnList = "user_id")
        })
public class PredictionRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private LocalDateTime joinedAt;
}
