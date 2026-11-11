package com.example.scalys.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderLookupLimiter {
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> failures = new ConcurrentHashMap<>();

    public void check(String ip) {
        ArrayDeque<Instant> attempts = failures.computeIfAbsent(ip, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            trim(attempts);
            if (attempts.size() >= 10) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã thử quá nhiều lần. Vui lòng thử lại sau 10 phút.");
        }
    }

    public void fail(String ip) {
        ArrayDeque<Instant> attempts = failures.computeIfAbsent(ip, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            trim(attempts);
            attempts.addLast(Instant.now());
        }
    }

    private void trim(ArrayDeque<Instant> attempts) {
        Instant cutoff = Instant.now().minus(10, ChronoUnit.MINUTES);
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(cutoff)) attempts.removeFirst();
    }
}
