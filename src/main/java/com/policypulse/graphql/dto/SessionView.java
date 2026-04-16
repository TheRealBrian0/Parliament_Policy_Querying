package com.policypulse.graphql.dto;

import com.policypulse.domain.SessionStatus;
import com.policypulse.domain.SessionType;

public record SessionView(
        String id,
        int year,
        SessionType sessionType,
        SessionStatus status,
        String scrapeDate,
        String activatedAt
) {
}
