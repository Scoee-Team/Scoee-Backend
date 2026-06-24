package com.example.scoremate.domain.football.service;

import com.example.scoremate.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class FootballSyncScheduler {
    private final FootballMatchService footballMatchService;

    @Scheduled(cron = "${scoremate.scheduler.sync-upcoming-cron:0 0 4 * * *}")
    public void syncUpcomingMatchesDaily() {
        runSafely("sync upcoming matches", () -> footballMatchService.syncUpcoming(LocalDate.now().plusDays(1)));
    }

    @Scheduled(fixedDelayString = "${scoremate.scheduler.sync-today-delay-ms:1800000}")
    public void syncTodayMatches() {
        runSafely("sync today matches", () -> footballMatchService.syncUpcoming(LocalDate.now()));
    }

    private void runSafely(String jobName, Runnable runnable) {
        try {
            runnable.run();
        } catch (BusinessException e) {
            log.warn("{} skipped: {}", jobName, e.getErrorCode().getCode());
        } catch (Exception e) {
            log.error("{} failed", jobName, e);
        }
    }
}
