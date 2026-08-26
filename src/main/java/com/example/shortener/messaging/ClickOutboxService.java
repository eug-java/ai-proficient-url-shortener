package com.example.shortener.messaging;

import com.example.shortener.domain.UrlMapping;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClickOutboxService {
    public record Context(String ip,String userAgent,String referrer,String countryCode){}
    private final OutboxEventRepository outbox; private final ObjectMapper json; private final Clock clock;
    private final ClickEventProcessor processor; private final String mode; private final String pepper;
    public ClickOutboxService(OutboxEventRepository outbox,ObjectMapper json,Clock clock,ClickEventProcessor processor,
      @Value("${app.messaging.mode:kafka}")String mode,@Value("${app.analytics.ip-hash-pepper:change-me}")String pepper){
      this.outbox=outbox;this.json=json;this.clock=clock;this.processor=processor;this.mode=mode;this.pepper=pepper;
    }
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void record(UrlMapping mapping,Context context){
      var message=new ClickMessage(UUID.randomUUID(),mapping.getShortCode(),mapping.getOrganizationId(),
          mapping.getId(),clock.instant(),hash(context.ip()),context.userAgent(),context.referrer(),country(context.countryCode()));
      try {
        var event=new OutboxEvent(UUID.randomUUID(),"UrlMapping",mapping.getId().toString(),"LinkClicked",
            message.eventId(),json.writeValueAsString(message),clock.instant());
        outbox.save(event);
        if("inline".equalsIgnoreCase(mode)){processor.process(message);event.published(clock.instant());}
      } catch(JsonProcessingException e){throw new IllegalStateException("Cannot serialize click event",e);}
    }
    private String hash(String ip){if(ip==null||ip.isBlank())return null;try{
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((ip+pepper).getBytes(StandardCharsets.UTF_8)));
    }catch(Exception e){throw new IllegalStateException(e);}}
    private static String country(String value){return value!=null&&value.matches("(?i)[A-Z]{2}")?value.toUpperCase():null;}
}
