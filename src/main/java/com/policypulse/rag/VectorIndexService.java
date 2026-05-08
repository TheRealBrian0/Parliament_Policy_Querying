package com.policypulse.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class VectorIndexService {

    private static final Logger log = LoggerFactory.getLogger(VectorIndexService.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final OllamaEmbeddingModel embeddingModel;
    private final String chromaUrl;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String COLLECTION_NAME = "policy_pulse";

    // Improvement #3 — cache the collection UUID so we only call
    // GET /api/v1/collections/{name} once per JVM lifetime instead of
    // once per eviction run.
    private volatile String cachedCollectionId;

    public VectorIndexService(
            OllamaEmbeddingModel embeddingModel,
            @Value("${app.chromadb.url}") String chromaUrl) {
        this.embeddingModel = embeddingModel;
        this.chromaUrl = chromaUrl;
        this.embeddingStore = ChromaEmbeddingStore.builder()
                .baseUrl(chromaUrl)
                .collectionName(COLLECTION_NAME)
                .build();
    }

    public String add(long monthId, long documentId, int chunkIndex, String chunkText) {
        TextSegment segment = TextSegment.from(
                chunkText,
                dev.langchain4j.data.document.Metadata.from(
                        Map.of(
                                "monthId",    String.valueOf(monthId),
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

    public void deleteByMonthId(long monthId) {
        try {
            String payload = "{\"where\": {\"monthId\": \"" + monthId + "\"}}";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            String collectionId = resolveCollectionId();
            if (collectionId != null) {
                restTemplate.postForEntity(chromaUrl + "/api/v1/collections/" + collectionId + "/delete", request, String.class);
                log.info("Deleted vectors for monthId: {} from ChromaDB", monthId);
            } else {
                log.warn("Collection {} not found in ChromaDB, skipping deletion for monthId: {}", COLLECTION_NAME, monthId);
            }
        } catch (Exception e) {
            log.error("Failed to delete vectors for monthId: {}", monthId, e);
        }
    }

    /** Returns the cached collection UUID, fetching it from ChromaDB on first call. */
    private String resolveCollectionId() {
        if (cachedCollectionId == null) {
            cachedCollectionId = getCollectionId(COLLECTION_NAME);
        }
        return cachedCollectionId;
    }

    private String getCollectionId(String collectionName) {
        try {
            // GET /api/v1/collections/policy_pulse
            Map<String, Object> response = restTemplate.getForObject(chromaUrl + "/api/v1/collections/" + collectionName, Map.class);
            if (response != null && response.containsKey("id")) {
                return (String) response.get("id");
            }
        } catch (Exception e) {
            log.warn("Failed to get collection ID for {}", collectionName, e);
        }
        return null;
    }
}
