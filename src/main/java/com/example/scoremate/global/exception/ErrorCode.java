package com.example.scoremate.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "인증이 필요합니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "INVALID_OAUTH_PROVIDER", "지원하지 않는 로그인 방식입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    
    // Domain
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCH_NOT_FOUND", "경기를 찾을 수 없습니다."),
    LEAGUE_NOT_FOUND(HttpStatus.NOT_FOUND, "LEAGUE_NOT_FOUND", "리그를 찾을 수 없습니다."),
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_NOT_FOUND", "팀을 찾을 수 없습니다."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "방을 찾을 수 없습니다."),
    ROOM_ALREADY_JOINED(HttpStatus.BAD_REQUEST, "ROOM_ALREADY_JOINED", "이미 참여한 방입니다."),
    ALREADY_JOINED(HttpStatus.BAD_REQUEST, "ALREADY_JOINED", "이미 참여한 예측방입니다."),
    ROOM_CANCELLED(HttpStatus.BAD_REQUEST, "ROOM_CANCELLED", "취소된 방입니다."),
    ROOM_FULL(HttpStatus.BAD_REQUEST, "ROOM_FULL", "예측방 정원이 가득 찼어요."),
    ROOM_LOCKED(HttpStatus.BAD_REQUEST, "ROOM_LOCKED", "마감된 예측방입니다."),
    NOT_ROOM_PARTICIPANT(HttpStatus.FORBIDDEN, "NOT_ROOM_PARTICIPANT", "예측방 참여자가 아닙니다."),
    MATCH_NOT_IN_ROOM(HttpStatus.BAD_REQUEST, "MATCH_NOT_IN_ROOM", "방에 포함되지 않은 경기입니다."),
    MATCH_LOCKED(HttpStatus.BAD_REQUEST, "MATCH_LOCKED", "경기 시작 후에는 예측을 수정할 수 없어요."),
    PREDICTION_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "PREDICTION_DEADLINE_PASSED", "예측 마감 시간이 지나 수정할 수 없습니다."),
    PREDICTION_CLOSED(HttpStatus.BAD_REQUEST, "PREDICTION_CLOSED", "예측 마감 시간이 지나 수정할 수 없어요."),
    INVALID_SCORE(HttpStatus.BAD_REQUEST, "INVALID_SCORE", "잘못된 점수입니다."),
    INVALID_ROOM_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_ROOM_REQUEST", "예측방 요청이 올바르지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "요청 값이 올바르지 않습니다."),
    EXTERNAL_FOOTBALL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_FOOTBALL_API_ERROR", "외부 축구 데이터 API 오류입니다."),
    RESULT_NOT_READY(HttpStatus.BAD_REQUEST, "RESULT_NOT_READY", "결과가 아직 준비되지 않았습니다."),
    
    // Global
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
