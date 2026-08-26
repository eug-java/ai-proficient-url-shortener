package com.example.shortener.cache;

import com.example.shortener.domain.UrlMapping;
import com.example.shortener.persistence.UrlMappingRepository;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedirectCacheService {

    private final StringRedisTemplate redis;
    private final UrlMappingRepository repository;
    private final boolean enabled;

    public RedirectCacheService(
            ObjectProvider<StringRedisTemplate> redis,
            UrlMappingRepository repository,
            @Value("${app.cache.enabled:true}") boolean enabled
    ) {
        this.redis = redis.getIfAvailable();
        this.repository = repository;
        this.enabled = enabled && this.redis != null;
    }

    public Optional<UrlMapping> find(String code) {
        if (enabled) {
            String id = redis.opsForValue().get("redirect:" + code);
            if (id != null) {
                return repository.findById(UUID.fromString(id));
            }
        }
        Optional<UrlMapping> result = repository.findByShortCode(code);
        if (enabled && result.isPresent() && result.get().getStatus() == UrlMapping.Status.ACTIVE) {
            redis.opsForValue().set(
                    "redirect:" + code,
                    result.get().getId().toString(),
                    Duration.ofMinutes(10)
            );
        }
        return result;
    }

    public void evict(String code) {
        if (enabled) {
            redis.delete("redirect:" + code);
        }
    }
}
