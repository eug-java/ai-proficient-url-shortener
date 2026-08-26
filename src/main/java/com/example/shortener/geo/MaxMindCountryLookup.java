package com.example.shortener.geo;

import java.net.InetAddress;
import java.util.Optional;

@FunctionalInterface
public interface MaxMindCountryLookup {
    Optional<String> countryIso(InetAddress address);
}
