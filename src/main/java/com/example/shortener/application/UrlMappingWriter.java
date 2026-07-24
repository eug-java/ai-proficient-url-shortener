package com.example.shortener.application;

import com.example.shortener.domain.UrlMapping;
import com.example.shortener.persistence.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlMappingWriter {

    private final UrlMappingRepository repository;

    public UrlMappingWriter(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UrlMapping insert(UrlMapping mapping) {
        return repository.saveAndFlush(mapping);
    }
}
