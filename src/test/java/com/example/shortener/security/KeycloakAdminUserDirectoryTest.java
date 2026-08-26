package com.example.shortener.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakAdminUserDirectoryTest {

    @Test
    void createsUserWhenMissingAndSkipsEmailWhenDisabled() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://keycloak.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();

        KeycloakAdminProperties props = new KeycloakAdminProperties();
        props.setEnabled(true);
        props.setServerUrl("http://keycloak.test");
        props.setRealm("shortener");
        props.setClientId("shortener-admin");
        props.setClientSecret("secret");
        props.setSendInviteEmail(false);

        server.expect(requestTo("http://keycloak.test/realms/shortener/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"tok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.test/admin/realms/shortener/users?email=new@example.com&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.test/admin/realms/shortener/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .location(URI.create("http://keycloak.test/admin/realms/shortener/users/user-123")));

        KeycloakAdminUserDirectory directory = new KeycloakAdminUserDirectory(props, rest);
        KeycloakUserDirectory.DirectoryUser user = directory.findOrInviteByEmail("new@example.com");

        assertThat(user.sub()).isEqualTo("user-123");
        assertThat(user.email()).isEqualTo("new@example.com");
        assertThat(user.displayName()).isEqualTo("new");
        server.verify();
    }

    @Test
    void returnsExistingUserWithoutCreating() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://keycloak.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient rest = builder.build();

        KeycloakAdminProperties props = new KeycloakAdminProperties();
        props.setEnabled(true);
        props.setServerUrl("http://keycloak.test");
        props.setRealm("shortener");
        props.setClientId("shortener-admin");
        props.setClientSecret("secret");

        server.expect(requestTo("http://keycloak.test/realms/shortener/protocol/openid-connect/token"))
                .andRespond(withSuccess("{\"access_token\":\"tok\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://keycloak.test/admin/realms/shortener/users?email=demo@example.com&exact=true"))
                .andRespond(withSuccess(
                        """
                        [{"id":"existing-sub","email":"demo@example.com","firstName":"Demo","lastName":"Owner"}]
                        """,
                        MediaType.APPLICATION_JSON
                ));

        KeycloakAdminUserDirectory directory = new KeycloakAdminUserDirectory(props, rest);
        KeycloakUserDirectory.DirectoryUser user = directory.findOrInviteByEmail("demo@example.com");

        assertThat(user.sub()).isEqualTo("existing-sub");
        assertThat(user.displayName()).isEqualTo("Demo Owner");
        server.verify();
    }
}
