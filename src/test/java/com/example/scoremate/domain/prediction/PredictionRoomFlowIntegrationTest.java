package com.example.scoremate.domain.prediction;

import com.example.scoremate.domain.football.entity.FootballMatch;
import com.example.scoremate.domain.football.entity.League;
import com.example.scoremate.domain.football.entity.MatchStatus;
import com.example.scoremate.domain.football.entity.Team;
import com.example.scoremate.domain.football.repository.FootballMatchRepository;
import com.example.scoremate.domain.football.repository.LeagueRepository;
import com.example.scoremate.domain.football.repository.TeamRepository;
import com.example.scoremate.domain.prediction.dto.PredictionDtos.SubmitPredictionsRequest;
import com.example.scoremate.domain.prediction.dto.PredictionDtos.ScorePredictionRequest;
import com.example.scoremate.domain.predictionroom.dto.PredictionRoomDtos.CreatePredictionRoomRequest;
import com.example.scoremate.domain.predictionroom.dto.PredictionRoomDtos.JoinPredictionRoomRequest;
import com.example.scoremate.domain.predictionroom.entity.PredictionDeadlineType;
import com.example.scoremate.domain.predictionroom.entity.PredictionRoomType;
import com.example.scoremate.domain.predictionroom.repository.PredictionRoomRepository;
import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.entity.UserRole;
import com.example.scoremate.domain.user.repository.UserRepository;
import com.example.scoremate.global.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:scoremate-flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.cache.type=none",
        "spring.jpa.show-sql=false"
})
class PredictionRoomFlowIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired LeagueRepository leagueRepository;
    @Autowired TeamRepository teamRepository;
    @Autowired FootballMatchRepository matchRepository;
    @Autowired PredictionRoomRepository roomRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;

    @Test
    void createJoinSubmitAndCalculateResult() throws Exception {
        User host = saveUser("host", "호스트");
        User guest = saveUser("guest", "게스트");
        FootballMatch match = saveMatch();

        String createBody = objectMapper.writeValueAsString(new CreatePredictionRoomRequest(
                "결승전 예측",
                PredictionRoomType.SINGLE_MATCH,
                List.of(match.getId()),
                10,
                "INVITE_ONLY",
                PredictionDeadlineType.MATCH_KICKOFF,
                null
        ));

        String createResponse = mockMvc.perform(post("/api/v1/prediction-rooms")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(host))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn().getResponse().getContentAsString();

        Long roomId = objectMapper.readTree(createResponse).path("data").path("id").asLong();
        String inviteCode = roomRepository.findById(roomId).orElseThrow().getInviteCode();

        mockMvc.perform(post("/api/v1/prediction-rooms/join")
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(guest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new JoinPredictionRoomRequest(inviteCode))))
                .andExpect(status().isOk());

        submit(roomId, host, match.getId(), 2, 1);
        submit(roomId, guest, match.getId(), 0, 3);

        match.setStatus(MatchStatus.FINISHED);
        match.setHomeScore(2);
        match.setAwayScore(1);
        matchRepository.save(match);

        String resultResponse = mockMvc.perform(post("/api/v1/admin/prediction-rooms/{roomId}/calculate-result", roomId)
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(admin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loserUserId").value(guest.getId()))
                .andReturn().getResponse().getContentAsString();

        JsonNode participants = objectMapper.readTree(resultResponse).path("data").path("participants");
        assertThat(participants).hasSize(2);
    }

    private void submit(Long roomId, User user, Long matchId, int homeScore, int awayScore) throws Exception {
        SubmitPredictionsRequest request = new SubmitPredictionsRequest(List.of(new ScorePredictionRequest(matchId, homeScore, awayScore)));
        mockMvc.perform(put("/api/v1/prediction-rooms/{roomId}/predictions", roomId)
                        .header("Authorization", "Bearer " + jwtTokenProvider.createAccessToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private User saveUser(String subject, String nickname) {
        return userRepository.save(User.builder()
                .email(subject + "@example.com")
                .nickname(nickname)
                .role(UserRole.USER)
                .provider(OAuthProvider.KAKAO)
                .providerSubject(subject)
                .build());
    }

    private User admin() {
        return userRepository.save(User.builder()
                .email("admin@example.com")
                .nickname("관리자")
                .role(UserRole.ADMIN)
                .provider(OAuthProvider.KAKAO)
                .providerSubject("admin")
                .build());
    }

    private FootballMatch saveMatch() {
        League league = leagueRepository.save(League.builder().externalLeagueId(1L).name("World Cup").country("World").season(2026).build());
        Team home = teamRepository.save(Team.builder().externalTeamId(1L).name("Korea").shortName("KOR").league(league).build());
        Team away = teamRepository.save(Team.builder().externalTeamId(2L).name("Japan").shortName("JPN").league(league).build());
        return matchRepository.save(FootballMatch.builder()
                .externalMatchId(1L)
                .league(league)
                .homeTeam(home)
                .awayTeam(away)
                .kickoffTime(LocalDateTime.now().plusDays(1))
                .status(MatchStatus.SCHEDULED)
                .venue("Seoul")
                .build());
    }
}
