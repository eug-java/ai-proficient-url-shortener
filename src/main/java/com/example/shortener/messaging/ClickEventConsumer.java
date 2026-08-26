package com.example.shortener.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "kafka")
public class ClickEventConsumer {

    private final ClickEventProcessor processor;

    public ClickEventConsumer(ClickEventProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${app.kafka.clicks-topic:shortener.clicks.v1}",
            groupId = "${spring.kafka.consumer.group-id:shortener-analytics}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    public void consume(ClickMessage message) {
        processor.process(message);
    }
}
