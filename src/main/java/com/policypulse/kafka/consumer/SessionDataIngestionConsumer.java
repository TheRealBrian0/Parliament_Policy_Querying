package com.policypulse.kafka.consumer;

import com.policypulse.kafka.event.SessionScrapedEvent;
import com.policypulse.persistence.entity.SessionChunkEntity;
import com.policypulse.persistence.repository.SessionChunkRepository;
import com.policypulse.rag.InMemoryVectorIndexService;
import com.policypulse.rag.TextChunker;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SessionDataIngestionConsumer {

    private static final Logger log = LoggerFactory.getLogger(SessionDataIngestionConsumer.class);
    private final TextChunker textChunker;
    private final InMemoryVectorIndexService vectorIndexService;
    private final SessionChunkRepository sessionChunkRepository;

    public SessionDataIngestionConsumer(
            TextChunker textChunker,
            InMemoryVectorIndexService vectorIndexService,
            SessionChunkRepository sessionChunkRepository
    ) {
        this.textChunker = textChunker;
        this.vectorIndexService = vectorIndexService;
        this.sessionChunkRepository = sessionChunkRepository;
    }

    @KafkaListener(topics = "${app.topics.session-data-scraped}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onSessionData(SessionScrapedEvent event) {
        List<String> chunks = textChunker.chunk(event.rawText());
        int stored = 0;
        for (int i = 0; i < chunks.size(); i++) {
            if (sessionChunkRepository.existsByDocumentIdAndChunkIndex(event.documentId(), i)) {
                continue;
            }
            String vectorRef = vectorIndexService.add(event.sessionId(), event.documentId(), i, chunks.get(i));
            SessionChunkEntity entity = new SessionChunkEntity();
            entity.setSessionId(event.sessionId());
            entity.setDocumentId(event.documentId());
            entity.setChunkIndex(i);
            entity.setChunkText(chunks.get(i));
            entity.setVectorRef(vectorRef);
            sessionChunkRepository.save(entity);
            stored++;
        }
        log.info("Ingestion consumer processed: sessionId={}, documentId={}, chunksTotal={}, chunksStored={}",
                event.sessionId(), event.documentId(), chunks.size(), stored);
    }
}
