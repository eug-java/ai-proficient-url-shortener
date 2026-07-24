package com.example.shortener.application;

import com.example.shortener.persistence.UrlMappingRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsWriter {

    private final UrlMappingRepository repository;
    private final Clock clock;

    public AnalyticsWriter(UrlMappingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordClick(String shortCode) {
        repository.incrementAnalytics(shortCode, clock.instant());
    }
}
