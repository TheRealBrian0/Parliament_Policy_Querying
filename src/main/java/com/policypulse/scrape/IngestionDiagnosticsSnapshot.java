package com.policypulse.scrape;

import java.time.OffsetDateTime;

public record IngestionDiagnosticsSnapshot(
        OffsetDateTime lastRunAt,
        int rssFeedsChecked,
        int rssAccepted,
        int rssRejected,
        int prsPdfLinksChecked,
        int prsPdfAccepted,
        int prsPdfRejected
) {
    public static IngestionDiagnosticsSnapshot empty() {
        return new IngestionDiagnosticsSnapshot(null, 0, 0, 0, 0, 0, 0);
    }
}
