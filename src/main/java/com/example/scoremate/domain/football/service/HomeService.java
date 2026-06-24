package com.example.scoremate.domain.football.service;

import com.example.scoremate.domain.football.dto.HomeDtos.HomeResponse;
import com.example.scoremate.domain.notification.service.NotificationService;
import com.example.scoremate.domain.predictionroom.service.PredictionRoomService;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final CurrentUserProvider currentUserProvider;
    private final FootballMatchService footballMatchService;
    private final PredictionRoomService predictionRoomService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public HomeResponse home() {
        User user = currentUserProvider.getCurrentUser();
        var todayMatches = footballMatchService.todayTop(5);
        var rooms = predictionRoomService.myRooms(null, PageRequest.of(0, 5)).content();
        return new HomeResponse(todayMatches, todayMatches, rooms, todayMatches, notificationService.unreadCount(user.getId()));
    }
}
