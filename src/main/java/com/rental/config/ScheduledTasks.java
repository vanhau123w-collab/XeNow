package com.rental.config;

import com.rental.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Scheduled tasks for system maintenance.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Cleanup expired and revoked refresh tokens.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupExpiredRefreshTokens() {
        log.info("[SCHEDULER] Starting refresh token cleanup...");

        int expiredCount = refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
        int revokedCount = refreshTokenRepository.deleteByRevokedTrue();

        int totalDeleted = expiredCount + revokedCount;
        log.info("[SCHEDULER] Cleanup complete — deleted {} tokens (expired: {}, revoked: {})",
                totalDeleted, expiredCount, revokedCount);
    }
}
