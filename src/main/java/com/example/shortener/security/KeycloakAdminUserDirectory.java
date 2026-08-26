package com.example.shortener.security;

import com.example.shortener.domain.InvalidRequestException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.keycloak.admin.enabled", havingValue = "true")
public class KeycloakAdminUserDirectory implements KeycloakUserDirectory {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminUserDirectory.class);

    private final KeycloakAdminProperties props;
    private final RestClient rest;

    public KeycloakAdminUserDirectory(KeycloakAdminProperties props) {
        this.props = props;
        this.rest = RestClient.builder().baseUrl(trimSlash(props.getServerUrl())).build();
    }

    KeycloakAdminUserDirectory(KeycloakAdminProperties props, RestClient rest) {
        this.props = props;
        this.rest = rest;
    }

    @Override
    public DirectoryUser findOrInviteByEmail(String email) {
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidRequestException("Invite email is invalid");
        }
        try {
            String token = accessToken();
            DirectoryUser existing = findByEmail(token, normalized);
            if (existing != null) {
                return existing;
            }
            String userId = createUser(token, normalized);
            if (props.isSendInviteEmail()) {
                sendInviteEmail(token, userId);
            }
            String local = normalized.substring(0, normalized.indexOf('@'));
            return new DirectoryUser(userId, normalized, local);
        } catch (RestClientResponseException ex) {
            log.warn("Keycloak admin invite failed: status={} body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new InvalidRequestException("Unable to invite user via Keycloak");
        }
    }

    private String accessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        Map<String, Object> body = rest.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", props.getRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (body == null || body.get("access_token") == null) {
            throw new InvalidRequestException("Unable to authenticate with Keycloak Admin");
        }
        return body.get("access_token").toString();
    }

    private DirectoryUser findByEmail(String token, String email) {
        List<Map<String, Object>> users = rest.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users")
                        .queryParam("email", email)
                        .queryParam("exact", true)
                        .build(props.getRealm()))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (users == null || users.isEmpty()) {
            return null;
        }
        Map<String, Object> user = users.getFirst();
        String id = stringVal(user.get("id"));
        String display = displayName(user);
        return new DirectoryUser(id, email, display);
    }

    private String createUser(String token, String email) {
        String local = email.substring(0, email.indexOf('@'));
        var response = rest.post()
                .uri("/admin/realms/{realm}/users", props.getRealm())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "username", email,
                        "email", email,
                        "enabled", true,
                        "emailVerified", false,
                        "firstName", local,
                        "requiredActions", List.of("UPDATE_PASSWORD", "VERIFY_EMAIL")
                ))
                .retrieve()
                .toBodilessEntity();
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            DirectoryUser created = findByEmail(token, email);
            if (created == null) {
                throw new InvalidRequestException("Keycloak user created but id was not returned");
            }
            return created.sub();
        }
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private void sendInviteEmail(String token, String userId) {
        rest.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users/{id}/execute-actions-email")
                        .queryParam("client_id", props.getInviteClientId())
                        .queryParam("redirect_uri", props.getInviteRedirectUri())
                        .build(props.getRealm(), userId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"))
                .retrieve()
                .toBodilessEntity();
    }

    private static String displayName(Map<String, Object> user) {
        String first = stringVal(user.get("firstName"));
        String last = stringVal(user.get("lastName"));
        String joined = (first + " " + last).trim();
        if (!joined.isBlank()) {
            return joined;
        }
        String username = stringVal(user.get("username"));
        return username.isBlank() ? stringVal(user.get("email")) : username;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String trimSlash(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8081";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
