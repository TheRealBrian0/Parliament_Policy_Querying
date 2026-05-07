package com.policypulse.graphql.dto;

public record SystemStatusView(
        int currentYear,
        int currentMonth,
        int ingestedMonthCount,
        String statusMessage
) {
}
