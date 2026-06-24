package com.example.scoremate.domain.prediction.service;

import com.example.scoremate.domain.prediction.service.PredictionResultService.ParticipantStats;
import com.example.scoremate.domain.user.entity.OAuthProvider;
import com.example.scoremate.domain.user.entity.User;
import com.example.scoremate.domain.user.entity.UserRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviationCalculatorTest {

    private final DeviationCalculator calculator = new DeviationCalculator(10);

    @Test
    void calculatesSingleMatchDeviation() {
        assertThat(calculator.calculate(1, 3, 2, 1)).isEqualTo(3);
    }

    @Test
    void calculatesMultiMatchTotalDeviation() {
        int total = calculator.calculate(2, 1, 2, 1)
                + calculator.calculate(1, 0, 0, 0);

        assertThat(total).isEqualTo(1);
    }

    @Test
    void appliesMissingPredictionPenalty() {
        assertThat(calculator.missingPredictionPenalty()).isEqualTo(10);
    }

    @Test
    void tieBreakerPrefersMoreMissingPredictions() {
        PredictionResultService service = new PredictionResultService(null, null, null, null, null, null, calculator, null);

        List<ParticipantStats> losers = service.selectLosers(List.of(
                stats(1L, 10, 0, 10, LocalDateTime.now(), 1),
                stats(2L, 10, 1, 10, LocalDateTime.now().minusMinutes(1), 0)
        ));

        assertThat(losers).extracting(stat -> stat.user().getId()).containsExactly(2L);
    }

    @Test
    void tieBreakerKeepsMultipleLosersWhenStillTied() {
        LocalDateTime submittedAt = LocalDateTime.now();
        PredictionResultService service = new PredictionResultService(null, null, null, null, null, null, calculator, null);

        List<ParticipantStats> losers = service.selectLosers(List.of(
                stats(1L, 8, 0, 4, submittedAt, 2),
                stats(2L, 8, 0, 4, submittedAt, 2)
        ));

        assertThat(losers).hasSize(2);
    }

    private ParticipantStats stats(Long userId, int totalDeviation, int missingCount, int maxSingle, LocalDateTime lastSubmittedAt, int submittedCount) {
        User user = User.builder()
                .id(userId)
                .nickname("user" + userId)
                .role(UserRole.USER)
                .provider(OAuthProvider.KAKAO)
                .providerSubject(String.valueOf(userId))
                .build();
        return new ParticipantStats(user, totalDeviation, missingCount, maxSingle, lastSubmittedAt, submittedCount);
    }
}
