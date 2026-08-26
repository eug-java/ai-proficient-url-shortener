package com.example.shortener.geo;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GeoConfiguration.class);

    @Bean
    MaxMindCountryLookup maxMindCountryLookup(@Value("${app.geo.maxmind-db:}") String path) {
        if (path == null || path.isBlank()) {
            return address -> Optional.empty();
        }
        File file = new File(path);
        if (!file.isFile()) {
            log.warn("MaxMind DB not found at {}; country lookup via IP disabled", path);
            return address -> Optional.empty();
        }
        try {
            DatabaseReader reader = new DatabaseReader.Builder(file).build();
            log.info("Loaded MaxMind Country DB from {}", file.getAbsolutePath());
            return address -> lookup(reader, address);
        } catch (IOException ex) {
            log.warn("Failed to open MaxMind DB at {}: {}", path, ex.getMessage());
            return address -> Optional.empty();
        }
    }

    private static Optional<String> lookup(DatabaseReader reader, InetAddress address) {
        if (address == null || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()) {
            return Optional.empty();
        }
        try {
            CountryResponse response = reader.country(address);
            String iso = response.getCountry().getIsoCode();
            if (iso != null && iso.matches("(?i)[A-Z]{2}")) {
                return Optional.of(iso.toUpperCase());
            }
            return Optional.empty();
        } catch (GeoIp2Exception | IOException ex) {
            return Optional.empty();
        }
    }
}
