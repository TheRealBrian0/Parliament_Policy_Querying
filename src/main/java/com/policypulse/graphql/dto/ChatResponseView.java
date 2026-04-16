package com.policypulse.graphql.dto;

import com.policypulse.domain.SessionType;

import java.util.List;

public record ChatResponseView(
        String answer,
        List<String> sources,
        List<SessionType> sessionScope
) {
}
