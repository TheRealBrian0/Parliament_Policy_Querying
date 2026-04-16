package com.policypulse.scrape;

import java.time.OffsetDateTime;

public record MockScrapedDocument(
        Long documentId,
        String sourceUrl,
        String title,
        String rawText,
        OffsetDateTime publishedAt
) {
}
