package com.example.scalys.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailOutboxJob {
    private final EmailNotificationService notifications;

    public EmailOutboxJob(EmailNotificationService notifications) { this.notifications = notifications; }

    @Scheduled(fixedDelay = 60_000)
    public void deliver() {
        for (Long id : notifications.pendingIds()) {
            try {
                notifications.sendOne(id);
            } catch (RuntimeException ignored) {
                // Keep the outbox row for the next attempt; payment processing never depends on SMTP.
            }
        }
    }
}
