package com.policypulse.graphql.dto;

import java.util.List;

public record ChatResponseView(
        String answer,
        List<String> sources
) {
}
