package com.example.shortener.messaging;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="outbox_event")
public class OutboxEvent {
    @Id private UUID id;
    @Column(name="aggregate_type",nullable=false) private String aggregateType;
    @Column(name="aggregate_id",nullable=false) private String aggregateId;
    @Column(name="event_type",nullable=false) private String eventType;
    @Column(name="event_id",nullable=false,unique=true) private UUID eventId;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="published_at") private Instant publishedAt;
    protected OutboxEvent(){}
    public OutboxEvent(UUID id,String aggregateType,String aggregateId,String eventType,UUID eventId,
                       String payload,Instant createdAt){
        this.id=id;this.aggregateType=aggregateType;this.aggregateId=aggregateId;this.eventType=eventType;
        this.eventId=eventId;this.payload=payload;this.createdAt=createdAt;
    }
    public UUID getId(){return id;} public UUID getEventId(){return eventId;}
    public String getPayload(){return payload;} public Instant getCreatedAt(){return createdAt;}
    public void published(Instant at){publishedAt=at;}
}
