package com.policypulse.kafka.consumer;

import com.policypulse.kafka.event.DocumentScrapedEvent;
import com.policypulse.persistence.entity.SessionChunkEntity;
import com.policypulse.persistence.repository.SessionChunkRepository;
import com.policypulse.rag.VectorIndexService;
import com.policypulse.rag.TextChunker;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentIngestionConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionConsumer.class);
    private final TextChunker textChunker;
    private final VectorIndexService vectorIndexService;
    private final SessionChunkRepository sessionChunkRepository;

    public DocumentIngestionConsumer(
            TextChunker textChunker,
            VectorIndexService vectorIndexService,
            SessionChunkRepository sessionChunkRepository) {
        this.textChunker = textChunker;
        this.vectorIndexService = vectorIndexService;
        this.sessionChunkRepository = sessionChunkRepository;
    }

    @KafkaListener(topics = "${app.topics.session-data-scraped}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onDocumentScraped(DocumentScrapedEvent event) {
        List<String> chunks = textChunker.chunk(event.rawText());

        // Improvement #1 — one SELECT per document instead of one per chunk.
        // On the normal (first-time) ingest path there are zero duplicate chunks,
        // so we skip N per-chunk existence checks entirely.
        boolean documentAlreadySeen = sessionChunkRepository.existsByDocumentId(event.documentId());

        List<SessionChunkEntity> toSave = new ArrayList<>(chunks.size());
        int skipped = 0;

        for (int i = 0; i < chunks.size(); i++) {
            // On re-delivery (Kafka at-least-once), fall back to the fine-grained check.
            if (documentAlreadySeen &&
                    sessionChunkRepository.existsByDocumentIdAndChunkIndex(event.documentId(), i)) {
                skipped++;
                continue;
            }
            String vectorRef = vectorIndexService.add(event.monthId(), event.documentId(), i, chunks.get(i));
            SessionChunkEntity entity = new SessionChunkEntity();
            entity.setMonthId(event.monthId());
            entity.setDocumentId(event.documentId());
            entity.setChunkIndex(i);
            entity.setChunkText(chunks.get(i));
            entity.setVectorRef(vectorRef);
            toSave.add(entity);
        }

        // Improvement #2 — batch all new chunks into a single INSERT statement
        // instead of one round-trip per chunk. Requires
        // spring.jpa.properties.hibernate.jdbc.batch_size=50 in application.yml.
        if (!toSave.isEmpty()) {
            sessionChunkRepository.saveAll(toSave);
        }

        log.info("Ingestion consumer: monthId={}, documentId={}, year={}, month={}, " +
                        "chunksTotal={}, chunksStored={}, chunksSkipped={}",
                event.monthId(), event.documentId(), event.year(), event.month(),
                chunks.size(), toSave.size(), skipped);
    }
}
