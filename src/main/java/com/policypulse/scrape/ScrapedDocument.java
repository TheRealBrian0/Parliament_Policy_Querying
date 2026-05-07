package com.policypulse.scrape;

import java.time.OffsetDateTime;

/**
 * Represents a document successfully scraped and persisted for a given calendar month.
 */
public record ScrapedDocument(
        long documentId,
        String sourceUrl,
        String title,
        String rawText,
        OffsetDateTime publishedAt
) {
}
