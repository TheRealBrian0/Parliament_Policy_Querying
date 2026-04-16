package com.policypulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.topics")
public record AppTopicProperties(String sessionDataScraped) {
}
