package org.leoms.admin.security;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public void recordFailure(String address) {
        Deque<Instant> attempts = failures.computeIfAbsent(address, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            purge(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public boolean isBlocked(String address) {
        Deque<Instant> attempts = failures.get(address);
        if (attempts == null) return false;
        synchronized (attempts) {
            purge(attempts);
            return attempts.size() >= MAX_FAILURES;
        }
    }

    public void clear(String address) {
        failures.remove(address);
    }

    private void purge(Deque<Instant> attempts) {
        Instant cutoff = clock.instant().minus(WINDOW);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) {
            attempts.removeFirst();
        }
    }
}
