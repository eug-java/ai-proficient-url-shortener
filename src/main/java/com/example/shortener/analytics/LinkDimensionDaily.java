package com.example.shortener.analytics;
import jakarta.persistence.*; import java.io.Serializable; import java.time.LocalDate; import java.util.UUID;
@Entity @Table(name="link_dimension_daily") @IdClass(LinkDimensionDaily.Key.class)
public class LinkDimensionDaily {
 @Id @Column(name="url_mapping_id") private UUID urlMappingId; @Id private LocalDate day;
 @Id private String dimension; @Id @Column(name="dimension_value") private String dimensionValue;
 @Column(nullable=false) private long clicks; protected LinkDimensionDaily(){}
 public String getDimension(){return dimension;} public String getDimensionValue(){return dimensionValue;}
 public long getClicks(){return clicks;} public LocalDate getDay(){return day;}
 public record Key(UUID urlMappingId,LocalDate day,String dimension,String dimensionValue) implements Serializable{}
}
