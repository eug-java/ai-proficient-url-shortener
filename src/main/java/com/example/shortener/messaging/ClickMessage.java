package com.example.shortener.messaging;
import java.time.Instant; import java.util.UUID;
public record ClickMessage(UUID eventId,String shortCode,UUID orgId,UUID urlMappingId,Instant occurredAt,
                           String ipHash,String userAgent,String referrer,String countryCode){}
