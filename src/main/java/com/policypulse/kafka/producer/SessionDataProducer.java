package com.policypulse.kafka.producer;

import com.policypulse.config.AppTopicProperties;
import com.policypulse.kafka.event.SessionScrapedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class SessionDataProducer {

    private final KafkaTemplate<String, SessionScrapedEvent> kafkaTemplate;
    private final AppTopicProperties topics;

    public SessionDataProducer(KafkaTemplate<String, SessionScrapedEvent> kafkaTemplate, AppTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void publish(String key, SessionScrapedEvent event) {
        kafkaTemplate.send(topics.sessionDataScraped(), key, event);
    }
}
