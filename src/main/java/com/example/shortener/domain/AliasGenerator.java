package com.example.shortener.domain;

@FunctionalInterface
public interface AliasGenerator {
    String generate();
}
