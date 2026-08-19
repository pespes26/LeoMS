package org.leoms.admin.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {
    @Test
    void blocksFiveFailuresAndExpiresAfterWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptService attempts = new LoginAttemptService(clock);
        for (int i = 0; i < 5; i++) attempts.recordFailure("100.64.0.4");
        assertThat(attempts.isBlocked("100.64.0.4")).isTrue();
        clock.instant = clock.instant.plus(LoginAttemptService.WINDOW).plusSeconds(1);
        assertThat(attempts.isBlocked("100.64.0.4")).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");
        public ZoneId getZone() { return ZoneId.of("UTC"); }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant; }
    }
}
