package com.example.scoremate.domain.predictionroom.service;

import com.example.scoremate.domain.football.dto.FootballDtos.TeamResponse;
import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.football.service.FootballMatchService;
import com.example.scoremate.domain.notification.entity.NotificationType;
import com.example.scoremate.domain.notification.service.NotificationService;
import com.example.scoremate.domain.prediction.dto.PredictionStatus;
import com.example.scoremate.domain.prediction.entity.ScorePrediction;
import com.example.scoremate.domain.prediction.repository.ScorePredictionRepository;
import com.example.scoremate.domain.predictionroom.dto.PredictionRoomDtos.*;
import com.example.scoremate.domain.predictionroom.entity.*;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomMatchRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomParticipantRepository;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomRepository;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.global.exception.BusinessException;
import com.example.scoremate.global.exception.ErrorCode;
import com.example.scoremate.global.response.PageResponse;
import com.example.scoremate.global.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PredictionRoomService {
    private static final String INVITE_BASE_URL = "https://scoee.app/join/";
    private static final String INVITE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final PredictionRoomRepository roomRepository;
    private final PredictionRoomMatchRepository roomMatchRepository;
    private final PredictionRoomParticipantRepository participantRepository;
    private final ScorePredictionRepository scorePredictionRepository;
    private final FootballMatchService footballMatchService;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public CreatePredictionRoomResponse create(CreatePredictionRoomRequest request) {
        validateCreateRequest(request);
        User host = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomRepository.save(PredictionRoom.builder()
                .title(request.title())
                .type(request.type())
                .host(host)
                .inviteCode(generateInviteCode())
                .deadlineType(request.predictionDeadlineType())
                .status(PredictionRoomStatus.OPEN)
                .capacity(request.capacity() == null ? 20 : request.capacity())
                .build());

        for (Long matchId : request.matchIds()) {
            FootballMatch match = footballMatchService.getMatch(matchId);
            if (!match.getKickoffTime().isAfter(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.MATCH_LOCKED);
            }
            LocalDateTime deadline = request.predictionDeadlineType() == PredictionDeadlineType.SAME_DEADLINE
                    ? request.sameDeadline()
                    : match.getKickoffTime();
            if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.PREDICTION_CLOSED);
            }
            roomMatchRepository.save(PredictionRoomMatch.builder()
                    .room(room)
                    .footballMatch(match)
                    .predictionDeadline(deadline)
                    .build());
        }
        participantRepository.save(PredictionRoomParticipant.builder().room(room).user(host).joinedAt(LocalDateTime.now()).build());
        return new CreatePredictionRoomResponse(room.getId());
    }

    @Transactional
    public void join(JoinPredictionRoomRequest request) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = roomRepository.findByInviteCode(request.inviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        if (room.getStatus() == PredictionRoomStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ROOM_CANCELLED);
        }
        if (participantRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
            throw new BusinessException(ErrorCode.ALREADY_JOINED);
        }
        if (participantRepository.countByRoomId(room.getId()) >= room.getCapacity()) {
            throw new BusinessException(ErrorCode.ROOM_FULL);
        }
        participantRepository.save(PredictionRoomParticipant.builder().room(room).user(user).joinedAt(LocalDateTime.now()).build());
        if (!room.getHost().getId().equals(user.getId())) {
            notificationService.create(
                    room.getHost(),
                    NotificationType.FRIEND_JOINED,
                    "새 참여자가 들어왔어요.",
                    user.getNickname() + "님이 '" + room.getTitle() + "' 예측방에 참여했어요.",
                    "/rooms/" + room.getId()
            );
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PredictionRoomSummaryResponse> myRooms(PredictionRoomFilter filter, Pageable pageable) {
        User user = currentUserProvider.getCurrentUser();
        Page<PredictionRoomParticipant> page = participantRepository.findByUserId(user.getId(), pageable);
        List<PredictionRoomSummaryResponse> content = page.getContent().stream()
                .map(PredictionRoomParticipant::getRoom)
                .map(room -> summary(room, user.getId()))
                .filter(room -> filter == null || filter == PredictionRoomFilter.ALL
                        || (filter == PredictionRoomFilter.COMPLETED && room.status() == PredictionRoomStatus.COMPLETED)
                        || (filter == PredictionRoomFilter.NEEDS_INPUT && room.myPredictionStatus() != PredictionStatus.SUBMITTED))
                .toList();
        return new PageResponse<>(content, new PageResponse.PageInfo(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public PredictionRoomDetailResponse detail(Long roomId) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = getRoom(roomId);
        ensureParticipant(room.getId(), user.getId());
        return detailForUser(room, user.getId());
    }

    @Transactional(readOnly = true)
    public InviteLinkResponse inviteLink(Long roomId) {
        User user = currentUserProvider.getCurrentUser();
        PredictionRoom room = getRoom(roomId);
        ensureParticipant(room.getId(), user.getId());
        return new InviteLinkResponse(room.getId(), room.getInviteCode(), INVITE_BASE_URL + room.getInviteCode());
    }

    @Transactional(readOnly = true)
    public PredictionRoomDetailResponse invitePreview(String inviteCode) {
        PredictionRoom room = roomRepository.findByInviteCode(inviteCode).orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        return detailForUser(room, null);
    }

    public PredictionRoom getRoom(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
    }

    public void ensureParticipant(Long roomId, Long userId) {
        if (!participantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new BusinessException(ErrorCode.NOT_ROOM_PARTICIPANT);
        }
    }

    public PredictionRoomSummaryResponse summary(PredictionRoom room, Long userId) {
        List<PredictionRoomMatch> matches = roomMatchRepository.findByRoomId(room.getId());
        long submitted = userId == null ? 0 : scorePredictionRepository.countByRoomIdAndUserId(room.getId(), userId);
        PredictionStatus status = submitted == matches.size() ? PredictionStatus.SUBMITTED : PredictionStatus.NOT_SUBMITTED;
        LocalDateTime nextDeadline = matches.stream()
                .map(PredictionRoomMatch::getPredictionDeadline)
                .filter(deadline -> deadline.isAfter(LocalDateTime.now()))
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new PredictionRoomSummaryResponse(
                room.getId(),
                room.getTitle(),
                room.getType(),
                new HostResponse(room.getHost().getId(), room.getHost().getNickname(), room.getHost().getProfileImageUrl()),
                matches.size(),
                participantRepository.countByRoomId(room.getId()),
                room.getCapacity(),
                deriveStatus(room, matches),
                status,
                submitted + "/" + matches.size(),
                nextDeadline
        );
    }

    private PredictionRoomDetailResponse detailForUser(PredictionRoom room, Long userId) {
        List<PredictionRoomMatch> matches = roomMatchRepository.findByRoomId(room.getId());
        Map<Long, ScorePrediction> myPredictions = userId == null ? Map.of() : scorePredictionRepository.findByRoomIdAndUserId(room.getId(), userId).stream()
                .collect(Collectors.toMap(p -> p.getFootballMatch().getId(), Function.identity()));
        List<ParticipantResponse> participants = participantRepository.findByRoomId(room.getId()).stream()
                .map(p -> new ParticipantResponse(
                        p.getUser().getId(),
                        p.getUser().getNickname(),
                        p.getUser().getProfileImageUrl(),
                        (int) scorePredictionRepository.countByRoomIdAndUserId(room.getId(), p.getUser().getId()),
                        matches.size()))
                .toList();
        List<RoomMatchResponse> roomMatches = matches.stream().map(roomMatch -> {
            FootballMatch match = roomMatch.getFootballMatch();
            ScorePrediction prediction = myPredictions.get(match.getId());
            boolean locked = roomMatch.isLocked(LocalDateTime.now());
            return new RoomMatchResponse(
                    match.getId(),
                    match.getLeague().getName(),
                    TeamResponse.from(match.getHomeTeam()),
                    TeamResponse.from(match.getAwayTeam()),
                    match.getKickoffTime(),
                    roomMatch.getPredictionDeadline(),
                    match.getStatus(),
                    predictionStatus(prediction, locked, match.isFinished()),
                    prediction == null ? null : new ScoreValueResponse(prediction.getPredictedHomeScore(), prediction.getPredictedAwayScore()),
                    locked
            );
        }).toList();
        long submitted = userId == null ? 0 : scorePredictionRepository.countByRoomIdAndUserId(room.getId(), userId);
        PredictionStatus myStatus = submitted == matches.size() ? PredictionStatus.SUBMITTED : PredictionStatus.NOT_SUBMITTED;
        LocalDateTime nextDeadline = matches.stream().map(PredictionRoomMatch::getPredictionDeadline).filter(d -> d.isAfter(LocalDateTime.now())).min(Comparator.naturalOrder()).orElse(null);
        return new PredictionRoomDetailResponse(
                room.getId(),
                room.getTitle(),
                room.getType(),
                deriveStatus(room, matches),
                new HostResponse(room.getHost().getId(), room.getHost().getNickname(), room.getHost().getProfileImageUrl()),
                room.getInviteCode(),
                INVITE_BASE_URL + room.getInviteCode(),
                participantRepository.countByRoomId(room.getId()),
                room.getCapacity(),
                new PredictionSummaryResponse((int) submitted, matches.size(), myStatus),
                nextDeadline,
                participants,
                roomMatches
        );
    }

    private PredictionStatus predictionStatus(ScorePrediction prediction, boolean locked, boolean finished) {
        if (finished) {
            return PredictionStatus.COMPLETED;
        }
        if (locked) {
            return PredictionStatus.LOCKED;
        }
        return prediction == null ? PredictionStatus.NOT_SUBMITTED : PredictionStatus.EDITABLE;
    }

    public PredictionRoomStatus deriveStatus(PredictionRoom room, List<PredictionRoomMatch> matches) {
        if (room.getStatus() == PredictionRoomStatus.CANCELLED || room.getStatus() == PredictionRoomStatus.COMPLETED) {
            return room.getStatus();
        }
        boolean allFinished = matches.stream().allMatch(m -> m.getFootballMatch().isFinished());
        if (allFinished && !matches.isEmpty()) {
            return PredictionRoomStatus.COMPLETED;
        }
        long lockedCount = matches.stream().filter(m -> m.isLocked(LocalDateTime.now())).count();
        if (lockedCount == 0) {
            return PredictionRoomStatus.OPEN;
        }
        if (lockedCount == matches.size()) {
            return PredictionRoomStatus.LOCKED;
        }
        return PredictionRoomStatus.PARTIALLY_LOCKED;
    }

    private void validateCreateRequest(CreatePredictionRoomRequest request) {
        if (request.type() == PredictionRoomType.SINGLE_MATCH && request.matchIds().size() != 1) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_REQUEST);
        }
        if (request.type() == PredictionRoomType.MULTI_MATCH && request.matchIds().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_REQUEST);
        }
        if (request.predictionDeadlineType() == PredictionDeadlineType.SAME_DEADLINE && request.sameDeadline() == null) {
            throw new BusinessException(ErrorCode.INVALID_ROOM_REQUEST);
        }
    }

    private String generateInviteCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                builder.append(INVITE_CHARS.charAt(secureRandom.nextInt(INVITE_CHARS.length())));
            }
            code = builder.toString();
        } while (roomRepository.existsByInviteCode(code));
        return code;
    }
}
