package com.policypulse.graphql.dto;

public record IngestionDiagnosticsView(
        String lastRunAt,
        Integer rssFeedsChecked,
        Integer rssAccepted,
        Integer rssRejected,
        Integer prsPdfLinksChecked,
        Integer prsPdfAccepted,
        Integer prsPdfRejected,
        Integer totalPublished
) {
}
