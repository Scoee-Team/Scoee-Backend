package com.example.scoremate.domain.notification.entity;

import com.example.scoremate.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class NotificationSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private boolean deadlineReminder;

    @Column(nullable = false)
    private boolean matchResult;

    @Column(nullable = false)
    private boolean friendJoined;
}
