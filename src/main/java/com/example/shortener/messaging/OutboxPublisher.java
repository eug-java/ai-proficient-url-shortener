package com.example.shortener.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class OutboxPublisher {

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, ClickMessage> kafka;
    private final ObjectMapper json;
    private final Clock clock;
    private final String topic;

    public OutboxPublisher(
            OutboxEventRepository repository,
            ObjectProvider<KafkaTemplate<String, ClickMessage>> kafka,
            ObjectMapper json,
            Clock clock,
            @Value("${app.kafka.clicks-topic:shortener.clicks.v1}") String topic
    ) {
        this.repository = repository;
        this.kafka = kafka.getIfAvailable();
        this.json = json;
        this.clock = clock;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox-poll-ms:1000}")
    @Transactional
    public void publish() throws Exception {
        if (kafka == null) {
            return;
        }
        for (OutboxEvent event : repository.findTop100ByPublishedAtIsNullOrderByCreatedAt()) {
            if (!"LinkClicked".equals(event.getEventType())) {
                continue;
            }
            ClickMessage message = json.readValue(event.getPayload(), ClickMessage.class);
            kafka.send(topic, message.shortCode(), message).get();
            event.published(clock.instant());
        }
    }
}
