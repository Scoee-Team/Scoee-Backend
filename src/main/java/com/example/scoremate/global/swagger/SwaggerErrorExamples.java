package com.example.scoremate.global.swagger;

public final class SwaggerErrorExamples {
    public static final String AUTH_AND_DOMAIN = """
            에러 응답 예시:
            - AUTH_REQUIRED: 인증이 필요합니다.
            - USER_NOT_FOUND: 사용자를 찾을 수 없습니다.
            - MATCH_NOT_FOUND: 경기를 찾을 수 없습니다.
            - ROOM_NOT_FOUND: 방을 찾을 수 없습니다.
            - ROOM_FULL: 예측방 정원이 가득 찼어요.
            - NOT_ROOM_PARTICIPANT: 예측방 참여자가 아닙니다.
            - PREDICTION_CLOSED: 예측 마감 시간이 지나 수정할 수 없어요.
            - INVALID_SCORE: 스코어는 0 이상 20 이하의 정수여야 합니다.
            - RESULT_NOT_READY: 경기 결과 업데이트가 지연되고 있어요.
            """;

    private SwaggerErrorExamples() {
    }
}
