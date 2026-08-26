package com.example.shortener.security;

/**
 * Resolves identity for org invites. Production uses Keycloak Admin; tests use a local stub.
 */
public interface KeycloakUserDirectory {

    record DirectoryUser(String sub, String email, String displayName) {}

    /**
     * Finds an existing Keycloak user by email or creates one and optionally emails an invite.
     */
    DirectoryUser findOrInviteByEmail(String email);
}
