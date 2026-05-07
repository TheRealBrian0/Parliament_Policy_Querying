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
        int stored = 0;
        for (int i = 0; i < chunks.size(); i++) {
            if (sessionChunkRepository.existsByDocumentIdAndChunkIndex(event.documentId(), i)) {
                continue;
            }
            String vectorRef = vectorIndexService.add(event.monthId(), event.documentId(), i, chunks.get(i));
            SessionChunkEntity entity = new SessionChunkEntity();
            entity.setMonthId(event.monthId());
            entity.setDocumentId(event.documentId());
            entity.setChunkIndex(i);
            entity.setChunkText(chunks.get(i));
            entity.setVectorRef(vectorRef);
            sessionChunkRepository.save(entity);
            stored++;
        }
        log.info("Ingestion consumer: monthId={}, documentId={}, year={}, month={}, chunksTotal={}, chunksStored={}",
                event.monthId(), event.documentId(), event.year(), event.month(), chunks.size(), stored);
    }
}
