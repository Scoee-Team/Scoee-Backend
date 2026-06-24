package com.example.scoremate.domain.predictionroom.entity;

import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(indexes = @Index(name = "idx_room_invite_code", columnList = "inviteCode"))
public class PredictionRoom extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionRoomType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private User host;

    @Column(nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionDeadlineType deadlineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionRoomStatus status;

    @Column(nullable = false)
    private Integer capacity;
}
