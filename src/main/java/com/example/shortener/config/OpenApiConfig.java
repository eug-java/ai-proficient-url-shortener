package com.example.shortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI urlShortenerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AI-Proficient URL Shortener API")
                .description("Create short URLs, redirect visitors, and retrieve click analytics.")
                .version("1.0.0")
                .contact(new Contact().name("Evgenii Buianov"))
                .license(new License().name("Assessment project")));
    }
}
