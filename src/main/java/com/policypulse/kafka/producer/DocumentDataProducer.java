package com.policypulse.kafka.producer;

import com.policypulse.config.AppTopicProperties;
import com.policypulse.kafka.event.DocumentScrapedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentDataProducer {

    private final KafkaTemplate<String, DocumentScrapedEvent> kafkaTemplate;
    private final AppTopicProperties topics;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentDataProducer(KafkaTemplate<String, DocumentScrapedEvent> kafkaTemplate, 
                                AppTopicProperties topics,
                                ApplicationEventPublisher eventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.eventPublisher = eventPublisher;
    }

    public void publish(String key, DocumentScrapedEvent event) {
        eventPublisher.publishEvent(new EventWrapper(key, event));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onDocumentScrapedAfterCommit(EventWrapper wrapper) {
        kafkaTemplate.send(topics.sessionDataScraped(), wrapper.key(), wrapper.event());
    }

    public record EventWrapper(String key, DocumentScrapedEvent event) {}
}
