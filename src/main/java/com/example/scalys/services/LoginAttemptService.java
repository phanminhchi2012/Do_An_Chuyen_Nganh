package com.example.scalys.services;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private int failures;
    private Instant lockedUntil;

    public synchronized boolean isLocked() {
        if (lockedUntil == null) return false;
        if (Instant.now().isBefore(lockedUntil)) return true;
        failures = 0;
        lockedUntil = null;
        return false;
    }

    public synchronized boolean recordFailure() {
        if (isLocked()) return true;
        failures++;
        if (failures >= MAX_FAILURES) {
            lockedUntil = Instant.now().plus(LOCK_DURATION);
            return true;
        }
        return false;
    }

    public synchronized void reset() {
        failures = 0;
        lockedUntil = null;
    }
}
