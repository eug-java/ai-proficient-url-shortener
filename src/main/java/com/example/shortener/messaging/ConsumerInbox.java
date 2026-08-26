package com.example.shortener.messaging;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="consumer_inbox")
public class ConsumerInbox {
 @Id @Column(name="event_id") private UUID eventId;
 @Column(name="consumer_name",nullable=false) private String consumerName;
 @Column(name="processed_at",nullable=false) private Instant processedAt;
 protected ConsumerInbox(){}
 public ConsumerInbox(UUID eventId,String consumerName,Instant processedAt){this.eventId=eventId;this.consumerName=consumerName;this.processedAt=processedAt;}
}
