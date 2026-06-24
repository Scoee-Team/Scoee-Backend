# Scoee Backend API Specification

이 문서는 현재 Flutter 프론트엔드 화면과 라우팅 기준으로 Spring Boot 백엔드에서 우선 구현해야 할 API를 정리한다.

Scoee는 친구 초대 기반 축구 스코어 예측 앱이다. 현금성 보상, 배당, odds, betting UX를 제공하지 않는다. API와 DB 용어도 `bet`, `odds`, `payout`, `reward`, `cash` 대신 `prediction`, `deviation`, `room`, `result`를 사용한다.

## 1. 기본 규칙

### Base URL

```http
/api/v1
```

### 인증

초기 프론트는 카카오, 구글, 애플 로그인 버튼만 있고 버튼 탭 시 임시로 홈으로 이동한다. 백엔드 연동 시 OAuth 로그인 성공 후 자체 JWT를 발급한다.

권장 헤더:

```http
Authorization: Bearer {accessToken}
Content-Type: application/json
```

### 시간

모든 날짜/시간은 ISO-8601 문자열로 반환한다.

```json
"2026-06-24T20:00:00+09:00"
```

### 공통 응답

단건:

```json
{
  "data": {}
}
```

목록:

```json
{
  "data": [],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3
  }
}
```

에러:

```json
{
  "code": "PREDICTION_CLOSED",
  "message": "예측 마감 시간이 지나 수정할 수 없어요."
}
```

### 주요 Enum

```text
OAuthProvider: KAKAO, GOOGLE, APPLE
MatchStatus: SCHEDULED, LIVE, FINISHED, PREDICTION_CLOSED
PredictionRoomType: SINGLE_MATCH, MULTI_MATCH
PredictionDeadlineType: MATCH_KICKOFF, SAME_DEADLINE, EACH_MATCH_KICKOFF
PredictionRoomStatus: OPEN, PARTIALLY_LOCKED, LOCKED, COMPLETED
PredictionStatus: NOT_SUBMITTED, SUBMITTED, EDITABLE, LOCKED, COMPLETED
NotificationType: DEADLINE_REMINDER, FRIEND_JOINED, MATCH_RESULT_FINALIZED, LOSER_SELECTED
```

## 2. 데이터 모델

### User

```json
{
  "id": 1,
  "nickname": "민우",
  "statusMessage": "이번 주 평균 편차 줄이기",
  "profileImageUrl": "https://cdn.example.com/users/1.png",
  "provider": "KAKAO"
}
```

### Team

```json
{
  "id": 1,
  "name": "Manchester City",
  "shortName": "맨시티",
  "logoUrl": "https://example.com/man-city.png"
}
```

### League

```json
{
  "id": 39,
  "name": "Premier League",
  "country": "England",
  "logoUrl": "https://example.com/epl.png",
  "season": 2026
}
```

### MatchSummary

```json
{
  "id": 1001,
  "league": {
    "id": 39,
    "name": "프리미어리그",
    "season": 2026
  },
  "homeTeam": {
    "id": 1,
    "name": "맨체스터 시티",
    "shortName": "맨시티",
    "logoUrl": "https://example.com/city.png"
  },
  "awayTeam": {
    "id": 2,
    "name": "아스널",
    "shortName": "아스널",
    "logoUrl": "https://example.com/arsenal.png"
  },
  "kickoffTime": "2026-06-24T20:00:00+09:00",
  "status": "SCHEDULED",
  "homeScore": null,
  "awayScore": null,
  "venue": "Etihad Stadium",
  "predictionDeadline": "2026-06-24T19:50:00+09:00",
  "myPredictionStatus": "NOT_SUBMITTED"
}
```

### PredictionRoomSummary

```json
{
  "id": 10,
  "title": "주말 프리미어리그 예측방",
  "type": "MULTI_MATCH",
  "host": {
    "id": 1,
    "nickname": "민우",
    "profileImageUrl": "https://cdn.example.com/users/1.png"
  },
  "matchCount": 4,
  "participantCount": 12,
  "capacity": 20,
  "status": "OPEN",
  "myPredictionStatus": "SUBMITTED",
  "myPredictionLabel": "2/5",
  "nextDeadline": "2026-06-24T19:50:00+09:00"
}
```

## 3. 인증 API

### 3.1 OAuth 로그인

프론트에서 OAuth SDK 또는 웹 플로우로 받은 인가 코드/토큰을 백엔드에 전달한다. 백엔드는 provider 검증 후 회원 생성 또는 로그인 처리한다.

```http
POST /api/v1/auth/oauth/login
```

Request:

```json
{
  "provider": "KAKAO",
  "authorizationCode": "oauth_authorization_code",
  "redirectUri": "scoee://oauth/kakao"
}
```

Response:

```json
{
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "isNewUser": false,
    "user": {
      "id": 1,
      "nickname": "민우",
      "statusMessage": "이번 주 평균 편차 줄이기",
      "profileImageUrl": null,
      "provider": "KAKAO"
    }
  }
}
```

### 3.2 토큰 재발급

```http
POST /api/v1/auth/token/refresh
```

Request:

```json
{
  "refreshToken": "jwt-refresh-token"
}
```

### 3.3 로그아웃

```http
POST /api/v1/auth/logout
```

## 4. 홈 API

홈 화면은 오늘 경기, 관심 팀 경기, 활성 예측방, 추천 경기를 한 번에 보여준다.

### 4.1 홈 요약

```http
GET /api/v1/home
```

Response:

```json
{
  "data": {
    "todayMatches": [],
    "favoriteTeamMatches": [],
    "activeRooms": [],
    "recommendedMatches": [],
    "unreadNotificationCount": 3
  }
}
```

각 배열의 경기 객체는 `MatchSummary`, 예측방 객체는 `PredictionRoomSummary`를 사용한다.

## 5. 경기 API

### 5.1 경기 목록 조회

경기 페이지는 날짜 선택, 리그 선택, 관심 팀만 보기, 상태 필터가 필요하다.

```http
GET /api/v1/matches?date=2026-06-24&leagueId=39&favoriteOnly=true&status=SCHEDULED&page=0&size=20
```

Query:

```text
date: yyyy-MM-dd, optional
from: ISO-8601, optional
to: ISO-8601, optional
leagueId: Long, optional
favoriteOnly: Boolean, default false
status: MatchStatus, optional
page, size
```

Response:

```json
{
  "data": [
    {
      "id": 1001,
      "league": {
        "id": 39,
        "name": "프리미어리그",
        "season": 2026
      },
      "homeTeam": {
        "id": 1,
        "name": "맨체스터 시티",
        "shortName": "맨시티",
        "logoUrl": null
      },
      "awayTeam": {
        "id": 2,
        "name": "아스널",
        "shortName": "아스널",
        "logoUrl": null
      },
      "kickoffTime": "2026-06-24T20:00:00+09:00",
      "status": "SCHEDULED",
      "homeScore": null,
      "awayScore": null,
      "venue": "Etihad Stadium",
      "predictionDeadline": "2026-06-24T19:50:00+09:00",
      "myPredictionStatus": "NOT_SUBMITTED"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 5.2 날짜별 경기 수

달력/날짜 칩에서 특정 날짜에 경기가 있는지 보여줄 때 사용한다.

```http
GET /api/v1/matches/calendar?from=2026-06-01&to=2026-06-30&leagueId=39&favoriteOnly=false
```

Response:

```json
{
  "data": [
    {
      "date": "2026-06-24",
      "matchCount": 8,
      "favoriteMatchCount": 2
    }
  ]
}
```

### 5.3 경기 상세

```http
GET /api/v1/matches/{matchId}
```

Response:

```json
{
  "data": {
    "id": 1001,
    "league": {
      "id": 39,
      "name": "프리미어리그",
      "season": 2026,
      "round": "37R"
    },
    "homeTeam": {
      "id": 1,
      "name": "맨체스터 시티",
      "shortName": "맨시티",
      "logoUrl": null
    },
    "awayTeam": {
      "id": 2,
      "name": "아스널",
      "shortName": "아스널",
      "logoUrl": null
    },
    "kickoffTime": "2026-06-24T20:00:00+09:00",
    "status": "SCHEDULED",
    "homeScore": null,
    "awayScore": null,
    "venue": "Etihad Stadium",
    "predictionDeadline": "2026-06-24T19:50:00+09:00",
    "locked": false,
    "activeRooms": [
      {
        "id": 10,
        "title": "주말 프리미어리그 예측방",
        "host": {
          "id": 1,
          "nickname": "민우",
          "profileImageUrl": null
        },
        "participantCount": 12,
        "capacity": 20,
        "status": "OPEN",
        "myPredictionStatus": "NOT_SUBMITTED"
      }
    ]
  }
}
```

### 5.4 리그 목록

```http
GET /api/v1/leagues?season=2026
```

### 5.5 팀 검색/목록

```http
GET /api/v1/teams?leagueId=39&query=맨시티&page=0&size=20
```

## 6. 관심 리그/팀 API

프로필의 관심 리그와 팀 설정, 경기 목록의 “관심 팀만 보기” 필터에 필요하다.

### 6.1 내 관심 목록

```http
GET /api/v1/favorites
```

Response:

```json
{
  "data": {
    "leagues": [],
    "teams": []
  }
}
```

### 6.2 관심 리그 추가/삭제

```http
POST /api/v1/favorites/leagues/{leagueId}
DELETE /api/v1/favorites/leagues/{leagueId}
```

### 6.3 관심 팀 추가/삭제

```http
POST /api/v1/favorites/teams/{teamId}
DELETE /api/v1/favorites/teams/{teamId}
```

## 7. 예측방 API

### 7.1 내 예측방 목록

예측방 페이지에서 전체, 입력 필요, 완료 필터가 필요하다.

```http
GET /api/v1/prediction-rooms/me?filter=ALL&page=0&size=20
```

Filter:

```text
ALL, NEEDS_INPUT, COMPLETED
```

Response:

```json
{
  "data": [
    {
      "id": 10,
      "title": "주말 프리미어리그 예측방",
      "type": "MULTI_MATCH",
      "host": {
        "id": 1,
        "nickname": "민우",
        "profileImageUrl": null
      },
      "matchCount": 4,
      "participantCount": 12,
      "capacity": 20,
      "status": "OPEN",
      "myPredictionStatus": "NOT_SUBMITTED",
      "myPredictionLabel": "2/5",
      "nextDeadline": "2026-06-24T19:50:00+09:00"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 7.2 예측방 생성

단일 경기와 여러 경기 모두 같은 endpoint를 사용한다.

```http
POST /api/v1/prediction-rooms
```

Single Match Request:

```json
{
  "title": "오늘 한일전 스코어 예측",
  "type": "SINGLE_MATCH",
  "matchIds": [1001],
  "capacity": 20,
  "visibility": "INVITE_ONLY",
  "predictionDeadlineType": "MATCH_KICKOFF"
}
```

Multi Match Request:

```json
{
  "title": "월드컵 조별리그 예측방",
  "type": "MULTI_MATCH",
  "matchIds": [1001, 1002, 1003, 1004],
  "capacity": 20,
  "visibility": "INVITE_ONLY",
  "predictionDeadlineType": "EACH_MATCH_KICKOFF",
  "sameDeadline": null
}
```

Response:

```json
{
  "data": {
    "id": 10
  }
}
```

Validation:

```text
title: 1~40자
matchIds: 최소 1개
SINGLE_MATCH: matchIds는 1개만 허용
MULTI_MATCH: matchIds는 2개 이상 권장
이미 시작한 경기 또는 예측 마감된 경기는 생성 불가
```

### 7.3 예측방 상세

```http
GET /api/v1/prediction-rooms/{roomId}
```

Response:

```json
{
  "data": {
    "id": 10,
    "title": "주말 프리미어리그 예측방",
    "type": "MULTI_MATCH",
    "status": "OPEN",
    "host": {
      "id": 1,
      "nickname": "민우",
      "profileImageUrl": null
    },
    "inviteCode": "AB12CD",
    "inviteLink": "https://scoee.app/join/AB12CD",
    "participantCount": 12,
    "capacity": 20,
    "myPredictionSummary": {
      "submittedCount": 2,
      "totalCount": 5,
      "status": "NOT_SUBMITTED"
    },
    "nextDeadline": "2026-06-24T19:50:00+09:00",
    "participants": [
      {
        "userId": 1,
        "nickname": "민우",
        "profileImageUrl": null,
        "submittedCount": 2,
        "totalCount": 5
      }
    ],
    "matches": [
      {
        "matchId": 1001,
        "leagueName": "프리미어리그 37R",
        "homeTeam": {
          "id": 1,
          "name": "맨체스터 시티",
          "shortName": "맨시티",
          "logoUrl": null
        },
        "awayTeam": {
          "id": 2,
          "name": "아스널",
          "shortName": "아스널",
          "logoUrl": null
        },
        "kickoffTime": "2026-06-24T20:00:00+09:00",
        "predictionDeadline": "2026-06-24T19:50:00+09:00",
        "matchStatus": "SCHEDULED",
        "predictionStatus": "NOT_SUBMITTED",
        "myPrediction": null,
        "locked": false
      }
    ]
  }
}
```

### 7.4 초대 링크 생성/조회

```http
POST /api/v1/prediction-rooms/{roomId}/invite-link
GET /api/v1/prediction-rooms/invite/{inviteCode}
```

### 7.5 초대 코드로 참여

```http
POST /api/v1/prediction-rooms/join
```

Request:

```json
{
  "inviteCode": "AB12CD"
}
```

Error cases:

```text
ROOM_NOT_FOUND
ALREADY_JOINED
ROOM_FULL
ROOM_LOCKED
```

## 8. 스코어 예측 API

### 8.1 내 예측 조회

스코어 입력 화면 진입 시 사용한다. 여러 경기 예측방이면 모든 대상 경기와 내 입력값을 반환한다.

```http
GET /api/v1/prediction-rooms/{roomId}/predictions/me
```

Response:

```json
{
  "data": {
    "roomId": 10,
    "roomTitle": "주말 프리미어리그 예측방",
    "status": "OPEN",
    "matches": [
      {
        "matchId": 1001,
        "leagueName": "프리미어리그 37R",
        "homeTeam": {
          "id": 1,
          "name": "맨체스터 시티",
          "shortName": "맨시티",
          "logoUrl": null
        },
        "awayTeam": {
          "id": 2,
          "name": "리버풀",
          "shortName": "리버풀",
          "logoUrl": null
        },
        "kickoffTime": "2026-06-24T20:00:00+09:00",
        "predictionDeadline": "2026-06-24T19:50:00+09:00",
        "locked": false,
        "predictionStatus": "NOT_SUBMITTED",
        "homeScore": null,
        "awayScore": null,
        "crowdSummary": {
          "homeWinPercent": 48,
          "drawPercent": 26,
          "awayWinPercent": 26
        }
      }
    ]
  }
}
```

### 8.2 예측 제출/수정

```http
PUT /api/v1/prediction-rooms/{roomId}/predictions
```

Request:

```json
{
  "predictions": [
    {
      "matchId": 1001,
      "homeScore": 2,
      "awayScore": 1
    }
  ]
}
```

Response:

```json
{
  "data": {
    "roomId": 10,
    "submittedCount": 1,
    "totalCount": 1,
    "status": "SUBMITTED"
  }
}
```

Validation:

```text
homeScore, awayScore: 0 이상 20 이하 정수
예측 마감 이후 수정 불가
방 참여자가 아닌 경우 제출 불가
COMPLETED 방에는 제출 불가
```

## 9. 결과 API

백엔드가 편차 계산과 꼴찌 선정의 source of truth다. 프론트는 반환값을 그대로 표시한다.

편차 공식:

```text
abs(predictedHomeScore - actualHomeScore) + abs(predictedAwayScore - actualAwayScore)
```

### 9.1 예측방 결과

```http
GET /api/v1/prediction-rooms/{roomId}/results
```

Response:

```json
{
  "data": {
    "roomId": 10,
    "roomTitle": "주말 프리미어리그 예측방",
    "roomStatus": "COMPLETED",
    "loserUserId": 3,
    "loserMessage": "총 편차 8점으로 가장 멀리 빗나갔어요.",
    "participants": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "지훈",
        "profileImageUrl": null,
        "totalDeviation": 1,
        "isBest": true,
        "isLoser": false,
        "submittedMatchCount": 4
      },
      {
        "rank": 4,
        "userId": 3,
        "nickname": "민우",
        "profileImageUrl": null,
        "totalDeviation": 8,
        "isBest": false,
        "isLoser": true,
        "submittedMatchCount": 4
      }
    ],
    "matchResults": [
      {
        "matchId": 1001,
        "leagueName": "프리미어리그",
        "homeTeam": {
          "id": 1,
          "name": "리버풀",
          "shortName": "리버풀",
          "logoUrl": null
        },
        "awayTeam": {
          "id": 2,
          "name": "첼시",
          "shortName": "첼시",
          "logoUrl": null
        },
        "actualHomeScore": 4,
        "actualAwayScore": 1,
        "predictions": [
          {
            "userId": 1,
            "predictedHomeScore": 3,
            "predictedAwayScore": 1,
            "deviation": 1
          }
        ]
      }
    ],
    "tiebreakerDescription": "동률일 경우 가장 늦게 제출한 사용자를 뒤 순위로 표시합니다."
  }
}
```

## 10. 랭킹 API

랭킹 화면은 “방 랭킹”과 “내 기록” 두 탭이 있다.

### 10.1 방 랭킹

```http
GET /api/v1/rankings/rooms/{roomId}
```

Response:

```json
{
  "data": {
    "roomId": 10,
    "roomTitle": "주말 프리미어리그 예측방",
    "reflectedMatchCount": 4,
    "bestDeviation": 1,
    "rows": [
      {
        "rank": 1,
        "userId": 1,
        "nickname": "지훈",
        "profileImageUrl": null,
        "totalDeviation": 1,
        "matchCount": 4,
        "averageDeviation": 0.25,
        "isBest": true,
        "isLoser": false
      }
    ]
  }
}
```

### 10.2 내 랭킹/기록

```http
GET /api/v1/rankings/me?period=MONTH
```

Period:

```text
ALL, MONTH, SEASON
```

Response:

```json
{
  "data": {
    "nickname": "민우",
    "overallRank": 12,
    "monthlyRank": 4,
    "activeRoomCount": 2,
    "averageDeviation": 2.4,
    "bestPredictionCount": 7,
    "loserCount": 1,
    "completedRoomCount": 8
  }
}
```

## 11. 프로필/내 정보 API

### 11.1 내 정보 조회

```http
GET /api/v1/users/me
```

Response:

```json
{
  "data": {
    "id": 1,
    "nickname": "민우",
    "statusMessage": "이번 주 평균 편차 줄이기",
    "profileImageUrl": null,
    "activeRoomCount": 2,
    "averageDeviation": 2.4,
    "provider": "KAKAO"
  }
}
```

### 11.2 내 프로필 수정

```http
PATCH /api/v1/users/me
```

Request:

```json
{
  "nickname": "민우",
  "statusMessage": "이번 주 평균 편차 줄이기"
}
```

Validation:

```text
nickname: 2~12자, 중복 허용 여부는 정책 결정
statusMessage: 0~40자
```

### 11.3 프로필 이미지 업로드

```http
POST /api/v1/users/me/profile-image
Content-Type: multipart/form-data
```

Form:

```text
file: image/png, image/jpeg, image/webp
```

Response:

```json
{
  "data": {
    "profileImageUrl": "https://cdn.example.com/users/1/profile.png"
  }
}
```

### 11.4 기본 프로필 이미지로 변경

```http
DELETE /api/v1/users/me/profile-image
```

## 12. 알림 API

홈에서만 알림을 진입점으로 사용한다. 경기 페이지 상단 알림 아이콘은 제거되어 있다.

### 12.1 알림 목록

```http
GET /api/v1/notifications?page=0&size=20
```

Response:

```json
{
  "data": [
    {
      "id": 1,
      "type": "DEADLINE_REMINDER",
      "title": "예측 마감 10분 전입니다.",
      "body": "아직 스코어를 입력하지 않은 경기가 있어요.",
      "read": false,
      "createdAt": "2026-06-24T19:40:00+09:00",
      "deepLink": "/rooms/10/predict"
    }
  ],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 12.2 읽음 처리

```http
PATCH /api/v1/notifications/{notificationId}/read
PATCH /api/v1/notifications/read-all
```

### 12.3 알림 설정 조회/수정

```http
GET /api/v1/users/me/notification-settings
PATCH /api/v1/users/me/notification-settings
```

Request:

```json
{
  "deadlineReminder": true,
  "matchResult": true,
  "friendJoined": false
}
```

## 13. 외부 축구 API 동기화

프론트에는 노출하지 않는 서버 내부/관리 기능이다.

### 권장 동기화 작업

```text
리그/시즌 동기화
팀 동기화
경기 일정 동기화
라이브 스코어/결과 동기화
경기 종료 후 예측방 결과 계산
마감 10분 전 알림 생성
결과 확정 후 꼴찌 선정 알림 생성
```

Spring Batch 또는 `@Scheduled` 작업으로 구성한다.

### 내부 관리자 endpoint 예시

```http
POST /api/v1/admin/sync/leagues
POST /api/v1/admin/sync/teams
POST /api/v1/admin/sync/matches?date=2026-06-24
POST /api/v1/admin/sync/results?date=2026-06-24
```

관리자 endpoint는 운영 환경에서 관리자 권한 또는 내부 네트워크로 제한한다.

## 14. 권장 Spring Boot 패키지 구조

```text
com.scoee.backend
  auth
    controller
    service
    dto
    domain
  user
  favorite
  football
    league
    team
    match
    external
  prediction
    room
    prediction
    result
  ranking
  notification
  common
    error
    security
    response
```

## 15. MVP 구현 우선순위

1. OAuth 로그인, JWT 발급, 내 정보 조회
2. 리그/팀/경기 데이터 저장 및 경기 목록/상세 조회
3. 관심 리그/팀 설정
4. 예측방 생성, 목록, 상세, 초대 참여
5. 스코어 예측 조회/제출/수정 및 마감 잠금
6. 경기 결과 동기화 후 편차 계산, 꼴찌 선정, 결과 조회
7. 방 랭킹과 내 기록 조회
8. 알림 목록/읽음 처리/설정

## 16. 주요 에러 코드

```text
UNAUTHORIZED: 로그인이 필요합니다.
INVALID_OAUTH_PROVIDER: 지원하지 않는 로그인 방식입니다.
MATCH_NOT_FOUND: 존재하지 않는 경기입니다.
MATCH_LOCKED: 경기 시작 후에는 예측을 수정할 수 없어요.
ROOM_NOT_FOUND: 존재하지 않는 예측방입니다.
ROOM_FULL: 예측방 정원이 가득 찼어요.
ROOM_LOCKED: 마감된 예측방입니다.
ALREADY_JOINED: 이미 참여한 예측방입니다.
NOT_ROOM_PARTICIPANT: 예측방 참여자가 아닙니다.
PREDICTION_CLOSED: 예측 마감 시간이 지나 수정할 수 없어요.
INVALID_SCORE: 스코어는 0 이상 20 이하의 정수여야 합니다.
RESULT_NOT_READY: 경기 결과 업데이트가 지연되고 있어요.
EXTERNAL_FOOTBALL_API_ERROR: 경기 데이터 제공처 응답이 지연되고 있어요.
```

## 17. 프론트 라우트와 필요한 API 매핑

```text
/login
  POST /auth/oauth/login

/
  GET /home
  GET /notifications 또는 unread count 포함

/matches
  GET /matches
  GET /matches/calendar
  GET /leagues
  GET /favorites

/matches/{matchId}
  GET /matches/{matchId}
  POST /prediction-rooms

/rooms
  GET /prediction-rooms/me

/rooms/create
  GET /matches
  POST /prediction-rooms

/rooms/{roomId}
  GET /prediction-rooms/{roomId}
  POST /prediction-rooms/{roomId}/invite-link

/rooms/{roomId}/predict
  GET /prediction-rooms/{roomId}/predictions/me
  PUT /prediction-rooms/{roomId}/predictions

/rooms/{roomId}/result
  GET /prediction-rooms/{roomId}/results

/ranking
  GET /rankings/rooms/{roomId}
  GET /rankings/me

/profile
  GET /users/me

/profile/edit
  PATCH /users/me
  POST /users/me/profile-image
  DELETE /users/me/profile-image

/profile/favorites
  GET /favorites
  POST/DELETE /favorites/leagues/{leagueId}
  POST/DELETE /favorites/teams/{teamId}

/profile/notifications
  GET /users/me/notification-settings
  PATCH /users/me/notification-settings
```
