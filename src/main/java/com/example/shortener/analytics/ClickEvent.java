package com.example.shortener.analytics;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="click_event")
public class ClickEvent {
 @Id private UUID id; @Column(name="event_id",nullable=false,unique=true) private UUID eventId;
 @Column(name="organization_id",nullable=false) private UUID organizationId;
 @Column(name="url_mapping_id",nullable=false) private UUID urlMappingId;
 @Column(name="short_code",nullable=false) private String shortCode;
 @Column(name="occurred_at",nullable=false) private Instant occurredAt;
 @Column(name="ip_hash") private String ipHash; @Column(name="user_agent") private String userAgent;
 @Column private String referrer;
 @Column(name="country_code", length = 2) private String countryCode;
 @Column(name="device_type") private String deviceType; @Column private String browser; @Column private String os;
 protected ClickEvent(){}
 public ClickEvent(UUID id,UUID eventId,UUID org,UUID mapping,String code,Instant at,String ip,String ua,
                   String ref,String country,String device,String browser,String os){
  this.id=id;this.eventId=eventId;this.organizationId=org;this.urlMappingId=mapping;this.shortCode=code;
  this.occurredAt=at;this.ipHash=ip;this.userAgent=ua;this.referrer=ref;this.countryCode=country;
  this.deviceType=device;this.browser=browser;this.os=os;
 }
}
