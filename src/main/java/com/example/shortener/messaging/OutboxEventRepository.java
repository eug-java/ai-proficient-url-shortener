package com.example.shortener.messaging;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAt();

    List<OutboxEvent> findTop100ByEventTypeAndPublishedAtIsNullOrderByCreatedAt(String eventType);
}
