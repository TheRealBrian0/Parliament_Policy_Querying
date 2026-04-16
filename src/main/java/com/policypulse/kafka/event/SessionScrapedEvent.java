package com.policypulse.kafka.event;

import com.policypulse.domain.SessionType;

import java.time.OffsetDateTime;

public record SessionScrapedEvent(
        long sessionId,
        long documentId,
        int year,
        SessionType sessionType,
        String sourceUrl,
        String title,
        String rawText,
        OffsetDateTime scrapedAt
) {
}
