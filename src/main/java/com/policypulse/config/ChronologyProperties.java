package com.policypulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.chronology")
public record ChronologyProperties(
        int session1Month,
        int session1Day,
        int session2Month,
        int session2Day,
        int session3Month,
        int session3Day
) {
}
