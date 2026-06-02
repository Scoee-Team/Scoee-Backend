# AGENTS.md — Spring Boot Backend

## 1. Project Overview

This project is a Spring Boot backend for a mobile football score prediction app.

The backend provides football match data, user management, favorite league/team management, prediction room creation, invite-based participation, score prediction submission, automatic deviation calculation, and loser selection.

The service is not a real-money betting service. Do not implement cash betting, odds, payout, gambling mechanics, cash-equivalent points, or reward exchange.

Use the term "prediction" rather than "betting" in code, API names, and documentation.

## 2. Core Domain Requirements

The app supports two prediction room types.

### 2.1 Single Match Prediction Room

A room contains one football match.

Participants submit one predicted score.

After the match result is finalized, the backend calculates the deviation for each participant and selects the participant with the largest deviation as the loser.

### 2.2 Multi-Match Prediction Room

A room contains multiple football matches.

This is required for competitions such as the World Cup, where users may want to predict multiple group-stage matches.

Participants submit predicted scores for each match in the room.

After all selected matches are finalized, the backend calculates each participant's total deviation and selects the participant with the largest total deviation as the loser.

## 3. Recommended Tech Stack

- Java 21 or Java 17
- Spring Boot 3.x
- Spring Web
- Spring Security
- Spring Data JPA
- QueryDSL if complex querying is needed
- MySQL or PostgreSQL
- Redis for caching external football API responses and invite codes if needed
- JWT authentication
- Gradle
- Docker Compose for local development
- OpenAPI/Swagger for API documentation

## 4. Package Structure

Use a domain-based package structure.

```text
src/main/java/com/example/scoremate/
  global/
    config/
    security/
    exception/
    response/
    util/
  domain/
    user/
      controller/
      service/
      repository/
      entity/
      dto/
    football/
      controller/
      service/
      repository/
      entity/
      dto/
      external/
    favorite/
      controller/
      service/
      repository/
      entity/
      dto/
    predictionroom/
      controller/
      service/
      repository/
      entity/
      dto/
    prediction/
      controller/
      service/
      repository/
      entity/
      dto/
    notification/
      service/
      dto/
```

## 5. External Football API Integration

The backend, not the Flutter client, should communicate directly with the external football data API.

Reasons:

- Hide external API keys from the frontend.
- Normalize data from external providers.
- Cache responses.
- Control rate limits.
- Maintain consistent internal IDs.

External API data to fetch:

- Leagues
- Teams
- Fixtures/matches
- Match status
- Final scores
- Competition matches, especially World Cup group-stage matches

Create a provider abstraction.

```java
public interface FootballDataProvider {
    List<ExternalMatchResponse> getUpcomingMatches(LocalDate date);
    List<ExternalMatchResponse> getMatchesByCompetition(Long competitionId, Integer season);
    ExternalMatchResponse getMatchDetail(Long externalMatchId);
}
```

Do not make the rest of the service layer depend directly on a specific provider such as API-Football, football-data.org, or Sportmonks.

## 6. Main Entities

### 6.1 User

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String nickname;
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 6.2 League

```java
@Entity
public class League {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalLeagueId;
    private String name;
    private String country;
    private String logoUrl;
}
```

### 6.3 Team

```java
@Entity
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalTeamId;
    private String name;
    private String shortName;
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    private League league;
}
```

### 6.4 Match

Use `FootballMatch` instead of `Match` to avoid naming ambiguity.

```java
@Entity
public class FootballMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long externalMatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    private League league;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    private Team awayTeam;

    private LocalDateTime kickoffTime;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    private Integer homeScore;
    private Integer awayScore;

    private String venue;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 6.5 PredictionRoom

```java
@Entity
public class PredictionRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Enumerated(EnumType.STRING)
    private PredictionRoomType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private User host;

    private String inviteCode;

    @Enumerated(EnumType.STRING)
    private PredictionDeadlineType deadlineType;

    @Enumerated(EnumType.STRING)
    private PredictionRoomStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 6.6 PredictionRoomMatch

This join entity allows one room to contain one or many matches.

```java
@Entity
public class PredictionRoomMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatch footballMatch;

    private LocalDateTime predictionDeadline;
}
```

### 6.7 PredictionRoomParticipant

```java
@Entity
public class PredictionRoomParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private LocalDateTime joinedAt;
}
```

### 6.8 ScorePrediction

One row represents one user's prediction for one match in one room.

```java
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_prediction_room_user_match",
            columnNames = {"room_id", "user_id", "football_match_id"}
        )
    }
)
public class ScorePrediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private PredictionRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private FootballMatch footballMatch;

    private Integer predictedHomeScore;
    private Integer predictedAwayScore;

    private Integer deviation;

    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;
}
```

### 6.9 PredictionRoomResult

```java
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

    private LocalDateTime calculatedAt;
}
```

## 7. Enums

```java
public enum PredictionRoomType {
    SINGLE_MATCH,
    MULTI_MATCH
}
```

```java
public enum PredictionDeadlineType {
    MATCH_KICKOFF,
    EACH_MATCH_KICKOFF,
    CUSTOM
}
```

```java
public enum PredictionRoomStatus {
    OPEN,
    PARTIALLY_LOCKED,
    LOCKED,
    COMPLETED,
    CANCELLED
}
```

```java
public enum MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    POSTPONED,
    CANCELLED
}
```

## 8. Deviation and Loser Selection Logic

The backend is the source of truth for deviation and loser selection.

### 8.1 Single Match Deviation

For each participant:

```text
deviation = abs(predictedHomeScore - actualHomeScore)
          + abs(predictedAwayScore - actualAwayScore)
```

Example:

```text
Actual: 2:1
Prediction: 1:3

Deviation = |1 - 2| + |3 - 1| = 1 + 2 = 3
```

The loser is the participant with the largest deviation.

### 8.2 Multi-Match Total Deviation

For each participant:

```text
totalDeviation = sum(matchDeviation for all finalized matches in the room)
```

The loser is the participant with the largest total deviation.

### 8.3 Missing Prediction Rule

For MVP, use the following rule:

If a participant does not submit a prediction for a match before the deadline, assign a fixed penalty deviation.

Recommended value:

```text
missingPredictionPenalty = 10
```

This value should be configurable.

For multi-match rooms:

```text
totalDeviation = submittedPredictionDeviationSum + missingPredictionPenaltySum
```

### 8.4 Tie-Breaker Rule

If multiple participants have the same highest total deviation, apply tie-breakers in this order:

1. More missing predictions
2. Larger maximum single-match deviation
3. Later final prediction submission time
4. If still tied, mark multiple losers

The API should support multiple losers if needed.

Recommended model change for multiple losers:

- Either use `PredictionRoomLoser` as a separate table
- Or keep `PredictionRoomResult` summary and return computed loser list in response

For MVP, multiple losers can be returned directly from service without storing each loser row.

## 9. Business Rules

### 9.1 Room Creation

- A room must have a title.
- A single-match room must contain exactly one match.
- A multi-match room must contain two or more matches.
- The host automatically joins the room.
- The room must be invite-only.
- Generate a unique invite code.

### 9.2 Joining a Room

- A user can join a room using an invite code.
- A user cannot join the same room twice.
- A user cannot join a cancelled room.
- A user may join an open or locked room, but cannot submit predictions for already locked matches.

### 9.3 Prediction Submission

- Scores must be non-negative integers.
- Scores must not exceed a reasonable maximum, such as 20.
- A user must be a participant in the room.
- The target match must belong to the room.
- A prediction cannot be submitted or edited after the match prediction deadline.
- In multi-match rooms, users may submit predictions match by match or all at once.

### 9.4 Prediction Lock

Prediction lock is match-specific, not only room-specific.

For multi-match rooms, some matches may be locked while future matches remain editable.

Room status rules:

```text
OPEN:
  All room matches are still editable.

PARTIALLY_LOCKED:
  At least one match is locked, but at least one match is still editable.

LOCKED:
  All matches are locked, but not all results are finalized.

COMPLETED:
  All matches are finalized and result calculation is complete.
```

### 9.5 Result Calculation

Calculate results when:

- A single-match room's match is finished.
- A multi-match room's all selected matches are finished.

The result calculation should be idempotent.

Calling result calculation multiple times must not create duplicate results or inconsistent loser records.

## 10. API Design

Base path:

```text
/api/v1
```

### 10.1 Matches

```http
GET /api/v1/matches/upcoming
GET /api/v1/matches/today
GET /api/v1/matches/{matchId}
GET /api/v1/competitions/{competitionId}/matches
```

Query examples:

```http
GET /api/v1/matches/upcoming?date=2026-06-15&leagueId=1
GET /api/v1/competitions/100/matches?season=2026&stage=GROUP
```

### 10.2 Favorites

```http
GET /api/v1/favorites
POST /api/v1/favorites/leagues/{leagueId}
DELETE /api/v1/favorites/leagues/{leagueId}
POST /api/v1/favorites/teams/{teamId}
DELETE /api/v1/favorites/teams/{teamId}
```

### 10.3 Prediction Rooms

```http
POST /api/v1/prediction-rooms
GET /api/v1/prediction-rooms/{roomId}
GET /api/v1/prediction-rooms/me
POST /api/v1/prediction-rooms/join
POST /api/v1/prediction-rooms/{roomId}/invite-link
```

### 10.4 Predictions

```http
POST /api/v1/prediction-rooms/{roomId}/predictions
PUT /api/v1/prediction-rooms/{roomId}/predictions
GET /api/v1/prediction-rooms/{roomId}/predictions/me
GET /api/v1/prediction-rooms/{roomId}/results
```

### 10.5 Admin or Batch APIs

These can be protected by admin authority or internal scheduling.

```http
POST /api/v1/admin/matches/sync
POST /api/v1/admin/matches/{matchId}/sync
POST /api/v1/admin/prediction-rooms/{roomId}/calculate-result
```

## 11. DTO Examples

### 11.1 Create Prediction Room Request

```java
public record CreatePredictionRoomRequest(
    String title,
    PredictionRoomType type,
    List<Long> matchIds,
    PredictionDeadlineType predictionDeadlineType
) {}
```

Validation:

- `title` must not be blank.
- `type` must not be null.
- `matchIds` must not be empty.
- If type is SINGLE_MATCH, `matchIds.size()` must be 1.
- If type is MULTI_MATCH, `matchIds.size()` must be greater than or equal to 2.

### 11.2 Join Room Request

```java
public record JoinPredictionRoomRequest(
    String inviteCode
) {}
```

### 11.3 Submit Predictions Request

```java
public record SubmitPredictionsRequest(
    List<ScorePredictionRequest> predictions
) {}

public record ScorePredictionRequest(
    Long matchId,
    Integer homeScore,
    Integer awayScore
) {}
```

Validation:

- `matchId` must not be null.
- `homeScore` and `awayScore` must be greater than or equal to 0.
- `homeScore` and `awayScore` must be less than or equal to 20.

### 11.4 Room Result Response

```java
public record PredictionRoomResultResponse(
    Long roomId,
    PredictionRoomStatus roomStatus,
    List<LoserResponse> losers,
    List<ParticipantDeviationResponse> participants,
    List<MatchPredictionResultResponse> matchResults
) {}

public record LoserResponse(
    Long userId,
    String nickname,
    Integer totalDeviation
) {}

public record ParticipantDeviationResponse(
    Long userId,
    String nickname,
    Integer totalDeviation,
    Integer missingPredictionCount,
    Boolean isLoser
) {}

public record MatchPredictionResultResponse(
    Long matchId,
    Integer actualHomeScore,
    Integer actualAwayScore,
    List<UserPredictionResultResponse> predictions
) {}

public record UserPredictionResultResponse(
    Long userId,
    String nickname,
    Integer predictedHomeScore,
    Integer predictedAwayScore,
    Integer deviation,
    Boolean missingPrediction
) {}
```

## 12. Service Layer Responsibilities

### 12.1 FootballMatchService

Responsibilities:

- Sync matches from external provider.
- Return upcoming matches.
- Return today's matches.
- Return competition matches.
- Update match status and scores.
- Normalize external API data into internal entities.

### 12.2 FavoriteService

Responsibilities:

- Add/remove favorite leagues.
- Add/remove favorite teams.
- Return user's favorite configuration.
- Return matches based on favorite teams/leagues.

### 12.3 PredictionRoomService

Responsibilities:

- Create single-match and multi-match rooms.
- Generate invite code.
- Join room by invite code.
- Return room detail.
- Return user's joined rooms.
- Determine room status based on match deadlines/results.

### 12.4 ScorePredictionService

Responsibilities:

- Submit predictions.
- Update predictions.
- Validate prediction deadline.
- Validate participant authorization.
- Validate match belongs to room.
- Return user's predictions.

### 12.5 PredictionResultService

Responsibilities:

- Calculate deviation.
- Apply missing prediction penalty.
- Apply tie-breaker rule.
- Select loser or losers.
- Return result detail.
- Ensure result calculation is idempotent.

## 13. Scheduler Requirements

Implement scheduled jobs if external API supports periodic sync.

Recommended jobs:

```text
- Sync upcoming matches once per day.
- Sync today's matches every 10 to 30 minutes.
- Sync live matches every 1 to 5 minutes if live status is supported.
- Recalculate completed prediction rooms after match results are updated.
```

Be careful with external API rate limits.

Cache external responses where appropriate.

## 14. Security Requirements

- Use JWT authentication.
- Only authenticated users can create rooms, join rooms, submit predictions, and view private room details.
- Users can only view rooms they participate in, unless future public rooms are explicitly implemented.
- Users cannot submit predictions for another user.
- Users cannot modify room settings unless they are the host.
- Admin sync APIs must require admin role or internal access.

## 15. Error Codes

Use consistent application error codes.

Examples:

```text
AUTH_REQUIRED
USER_NOT_FOUND
MATCH_NOT_FOUND
ROOM_NOT_FOUND
ROOM_ALREADY_JOINED
ROOM_CANCELLED
MATCH_NOT_IN_ROOM
PREDICTION_DEADLINE_PASSED
INVALID_SCORE
EXTERNAL_FOOTBALL_API_ERROR
RESULT_NOT_READY
```

Example error response:

```json
{
  "code": "PREDICTION_DEADLINE_PASSED",
  "message": "예측 마감 시간이 지나 수정할 수 없습니다."
}
```

## 16. Testing Requirements

### 16.1 Unit Tests

Write unit tests for:

- Single-match deviation calculation
- Multi-match total deviation calculation
- Missing prediction penalty
- Tie-breaker logic
- Prediction deadline validation
- Room creation validation

### 16.2 Integration Tests

Write integration tests for:

- Create room
- Join room
- Submit prediction
- Update prediction before deadline
- Reject update after deadline
- Calculate room result
- Multi-match room result

### 16.3 Important Test Cases

Single match:

```text
Actual: 2:1
A predicts 2:1 => deviation 0
B predicts 1:1 => deviation 1
C predicts 0:3 => deviation 4
Loser: C
```

Multi-match:

```text
Match 1 actual: 2:1
Match 2 actual: 0:0

A predicts:
  Match 1: 2:1 => 0
  Match 2: 1:0 => 1
  Total: 1

B predicts:
  Match 1: 0:3 => 4
  Match 2: 2:2 => 4
  Total: 8

Loser: B
```

Missing prediction:

```text
Penalty: 10
A misses one match => +10 deviation
```

## 17. Database Index Recommendations

Add indexes for:

- `FootballMatch.externalMatchId`
- `FootballMatch.kickoffTime`
- `PredictionRoom.inviteCode`
- `PredictionRoomParticipant.room_id`
- `PredictionRoomParticipant.user_id`
- `ScorePrediction.room_id`
- `ScorePrediction.user_id`
- `ScorePrediction.football_match_id`

## 18. Implementation Rules for Coding Agent

- Do not implement gambling, odds, payout, cash rewards, or real-money betting.
- Use "prediction" terminology in code and API names.
- Keep all business rules in service classes, not controllers.
- Use transactions for room creation, joining, prediction submission, and result calculation.
- Do not expose external football API keys.
- Do not let the frontend determine final deviation or loser.
- Use DTOs for API input/output.
- Avoid returning JPA entities directly.
- Validate all request bodies.
- Make result calculation idempotent.
- Support both single-match and multi-match rooms from the beginning.
- Design prediction lock at match level, not only room level.
- Use Korean error messages for user-facing messages if the frontend is Korean.
- Keep API response format consistent.

## 19. MVP Completion Criteria

The backend MVP is complete when:

- External match data can be synced or mocked.
- Users can view upcoming matches.
- Users can favorite leagues and teams.
- Users can create single-match prediction rooms.
- Users can create multi-match prediction rooms.
- Users can join rooms by invite code.
- Users can submit score predictions.
- Predictions are locked after deadline.
- Finished match results can be updated.
- Deviation is calculated correctly.
- The loser is selected correctly based on largest deviation.
- Missing predictions and ties are handled predictably.
