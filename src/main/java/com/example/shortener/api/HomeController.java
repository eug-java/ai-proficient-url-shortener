package com.example.shortener.api;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final Environment environment;

    public HomeController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/")
    public Map<String, Object> home() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "AI-Proficient URL Shortener");
        body.put("profiles", activeProfiles());
        body.put("createUrl", "POST /api/v1/urls");
        body.put("getUrl", "GET /api/v1/urls/{shortCode}");
        body.put("redirect", "GET /{shortCode}");
        body.put("analytics", "GET /api/v1/urls/{shortCode}/analytics");
        body.put("health", "GET /actuator/health");

        if (booleanProperty("springdoc.api-docs.enabled", false)) {
            body.put("openApi", "GET /v3/api-docs");
        }
        if (booleanProperty("springdoc.swagger-ui.enabled", false)) {
            body.put("swaggerUi", "GET /swagger-ui.html");
        }
        if (managementIncludes("prometheus")) {
            body.put("prometheus", "GET /actuator/prometheus");
        }
        return body;
    }

    private List<String> activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length > 0) {
            return Arrays.asList(profiles);
        }
        return Arrays.asList(environment.getDefaultProfiles());
    }

    private boolean booleanProperty(String key, boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    private boolean managementIncludes(String endpoint) {
        String included = environment.getProperty(
                "management.endpoints.web.exposure.include",
                "health"
        );
        return Arrays.stream(included.split(","))
                .map(String::trim)
                .anyMatch(endpoint::equalsIgnoreCase);
    }
}
