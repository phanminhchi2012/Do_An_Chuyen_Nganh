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
public class WarrantySubmissionLimiter {
    private final Map<String, ArrayDeque<Instant>> attempts = new HashMap<>();

    public synchronized void checkAndRecord(String address) {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(10));
        attempts.values().forEach(times -> {
            while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) times.removeFirst();
        });
        attempts.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        ArrayDeque<Instant> times = attempts.computeIfAbsent(address, ignored -> new ArrayDeque<>());
        if (times.size() >= 5)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã gửi nhiều yêu cầu. Vui lòng thử lại sau 10 phút.");
        times.addLast(Instant.now());
    }
}
