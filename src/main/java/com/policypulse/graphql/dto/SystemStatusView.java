package com.policypulse.graphql.dto;

import com.policypulse.domain.SessionType;

import java.util.List;

public record SystemStatusView(
        int currentYear,
        List<SessionType> activeSessions,
        Integer vectorContextYear,
        String lastIngestionAt
) {
}
