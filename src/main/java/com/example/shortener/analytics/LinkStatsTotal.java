package com.example.shortener.analytics;
import jakarta.persistence.*; import java.time.Instant; import java.util.UUID;
@Entity @Table(name="link_stats_total")
public class LinkStatsTotal {
 @Id @Column(name="url_mapping_id") private UUID urlMappingId;
 @Column(name="organization_id",nullable=false) private UUID organizationId;
 @Column(name="short_code",nullable=false) private String shortCode;
 @Column(name="total_clicks",nullable=false) private long totalClicks;
 @Column(name="last_clicked_at") private Instant lastClickedAt;
 @Column(name="unique_ip_hashes",nullable=false) private long uniqueIpHashes;
 protected LinkStatsTotal(){}
 public UUID getUrlMappingId(){return urlMappingId;} public long getTotalClicks(){return totalClicks;}
 public Instant getLastClickedAt(){return lastClickedAt;} public long getUniqueIpHashes(){return uniqueIpHashes;}
}
