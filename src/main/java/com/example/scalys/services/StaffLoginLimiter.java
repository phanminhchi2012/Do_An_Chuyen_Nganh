package com.example.scalys.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class StaffLoginLimiter {
    private record Attempt(int failures, Instant lockedUntil) {}
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public boolean locked(String key) {
        Attempt attempt = attempts.get(key);
        return attempt != null && attempt.lockedUntil() != null
                && attempt.lockedUntil().isAfter(Instant.now());
    }
    public void failed(String key) {
        attempts.compute(key, (ignored, old) -> {
            int failures = old == null || (old.lockedUntil() != null
                    && old.lockedUntil().isBefore(Instant.now())) ? 1 : old.failures() + 1;
            return new Attempt(failures, failures >= 5 ? Instant.now().plus(5, ChronoUnit.MINUTES) : null);
        });
    }
    public void success(String key) { attempts.remove(key); }
}
