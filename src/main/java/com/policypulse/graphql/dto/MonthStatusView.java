package com.policypulse.graphql.dto;

public record MonthStatusView(
        int year,
        int month,
        String ingestedAt
) {
}
