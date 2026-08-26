package com.example.shortener.persistence;

import com.example.shortener.domain.UrlMapping;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, UUID> {

    Optional<UrlMapping> findByShortCode(String shortCode);
    Optional<UrlMapping> findByOrganizationIdAndShortCode(UUID organizationId, String shortCode);
    java.util.List<UrlMapping> findAllByOrganizationIdAndStatusNotOrderByCreatedAtDesc(
            UUID organizationId, UrlMapping.Status status);

    @Modifying(clearAutomatically = true)
    @Query("""
            update UrlMapping u
            set u.totalClicks = u.totalClicks + 1,
                u.lastAccessedAt = :at,
                u.version = u.version + 1
            where u.shortCode = :code
            """)
    int incrementAnalytics(@Param("code") String code, @Param("at") Instant at);
}
