package com.example.scalys.services;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactSubmissionLimiter {
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_REQUESTS = 5;
    private final Map<String, ArrayDeque<Instant>> attempts = new HashMap<>();

    public synchronized void checkAndRecord(String remoteAddress) {
        Instant cutoff = Instant.now().minus(WINDOW);
        attempts.values().forEach(times -> {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) times.removeFirst();
        });
        attempts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ArrayDeque<Instant> times = attempts.computeIfAbsent(remoteAddress, ignored -> new ArrayDeque<>());
        if (times.size() >= MAX_REQUESTS)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã gửi nhiều lời nhắn. Vui lòng thử lại sau 10 phút.");
        times.addLast(Instant.now());
    }
}
