package com.policypulse.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import com.policypulse.persistence.entity.SessionChunkEntity;
import com.policypulse.persistence.repository.SessionChunkRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class InMemoryVectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(InMemoryVectorIndexService.class);

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private final OllamaEmbeddingModel embeddingModel;
    private final SessionChunkRepository sessionChunkRepository;

    public InMemoryVectorIndexService(OllamaEmbeddingModel embeddingModel, SessionChunkRepository sessionChunkRepository) {
        this.embeddingModel = embeddingModel;
        this.sessionChunkRepository = sessionChunkRepository;
    }

    @PostConstruct
    public void loadFromDatabaseOnStartup() {
        List<SessionChunkEntity> chunks = sessionChunkRepository.findAll();
        if (chunks.isEmpty()) {
            return;
        }
        log.info("Loading {} chunks from database into in-memory vector store...", chunks.size());
        int loaded = 0;
        for (SessionChunkEntity chunk : chunks) {
            try {
                add(chunk.getSessionId(), chunk.getDocumentId(), chunk.getChunkIndex(), chunk.getChunkText());
                loaded++;
            } catch (Exception ex) {
                log.warn("Failed to load chunk {} into vector store", chunk.getId(), ex);
            }
        }
        log.info("Successfully loaded {} chunks into in-memory vector store.", loaded);
    }

    public String add(long sessionId, long documentId, int chunkIndex, String chunkText) {
        TextSegment segment = TextSegment.from(
                chunkText,
                dev.langchain4j.data.document.Metadata.from(
                        Map.of(
                                "sessionId", String.valueOf(sessionId),
                                "documentId", String.valueOf(documentId),
                                "chunkIndex", String.valueOf(chunkIndex)
                        )
                )
        );
        Embedding embedding = embeddingModel.embed(chunkText).content();
        return embeddingStore.add(embedding, segment);
    }

    public List<EmbeddingMatch<TextSegment>> search(String query, int maxResults) {
        Embedding queryVector = embeddingModel.embed(query).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryVector)
                .maxResults(maxResults)
                .build();
        return embeddingStore.search(request).matches();
    }
}
