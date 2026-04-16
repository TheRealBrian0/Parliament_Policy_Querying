package com.policypulse.config;

import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaEmbeddingConfig {

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(
            @Value("${app.ollama.base-url}") String baseUrl,
            @Value("${app.ollama.embedding-model}") String embeddingModel
    ) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModel)
                .timeout(java.time.Duration.ofSeconds(30))
                .build();
    }
}
