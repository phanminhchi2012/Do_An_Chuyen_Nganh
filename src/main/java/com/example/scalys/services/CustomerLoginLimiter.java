package com.example.scalys.services;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CustomerLoginLimiter {
    private static final Duration LOCK_TIME = Duration.ofMinutes(5);
    private final Map<String, Attempt> attempts = new HashMap<>();

    public synchronized boolean locked(String key) {
        Attempt attempt = attempts.get(key);
        if (attempt == null) return false;
        if (attempt.lockedUntil != null && Instant.now().isBefore(attempt.lockedUntil)) return true;
        if (attempt.lockedUntil != null) attempts.remove(key);
        return false;
    }

    public synchronized void failed(String key) {
        if (attempts.size() > 10_000) attempts.entrySet().removeIf(entry ->
                entry.getValue().lockedUntil != null && Instant.now().isAfter(entry.getValue().lockedUntil));
        Attempt attempt = attempts.computeIfAbsent(key, ignored -> new Attempt());
        attempt.failures++;
        if (attempt.failures >= 5) attempt.lockedUntil = Instant.now().plus(LOCK_TIME);
    }

    public synchronized void success(String key) {
        attempts.remove(key);
    }

    private static final class Attempt {
        int failures;
        Instant lockedUntil;
    }
}
