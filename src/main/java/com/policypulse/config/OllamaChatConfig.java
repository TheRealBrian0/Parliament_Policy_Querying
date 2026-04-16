package com.policypulse.config;

import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OllamaChatConfig {

    @Bean
    public OllamaChatModel ollamaChatModel(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.chat-model}") String chatModel
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModel)
                .timeout(Duration.ofSeconds(180))
                .build();
    }
}
