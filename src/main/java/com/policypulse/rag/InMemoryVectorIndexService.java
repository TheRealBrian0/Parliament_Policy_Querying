package com.policypulse.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InMemoryVectorIndexService {

    private final EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();
    private final OllamaEmbeddingModel embeddingModel;

    public InMemoryVectorIndexService(OllamaEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
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
