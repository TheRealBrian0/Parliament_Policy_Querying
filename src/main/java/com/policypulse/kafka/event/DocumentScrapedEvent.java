package com.policypulse.kafka.event;

import java.time.OffsetDateTime;

public record DocumentScrapedEvent(
        long monthId,
        long documentId,
        int year,
        int month,
        String sourceUrl,
        String title,
        String rawText,
        OffsetDateTime scrapedAt
) {
}
