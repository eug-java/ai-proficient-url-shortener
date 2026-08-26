package com.example.shortener.analytics;
import jakarta.persistence.*; import java.io.Serializable; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="link_stats_daily") @IdClass(LinkStatsDaily.Key.class)
public class LinkStatsDaily {
 @Id @Column(name="url_mapping_id") private UUID urlMappingId; @Id private LocalDate day;
 @Column(name="organization_id",nullable=false) private UUID organizationId;
 @Column(name="short_code",nullable=false) private String shortCode;
 @Column(nullable=false) private long clicks; @Column(name="unique_ip_hashes",nullable=false) private long uniqueIpHashes;
 protected LinkStatsDaily(){}
 public LocalDate getDay(){return day;} public long getClicks(){return clicks;} public long getUniqueIpHashes(){return uniqueIpHashes;}
 public record Key(UUID urlMappingId,LocalDate day) implements Serializable{}
}
