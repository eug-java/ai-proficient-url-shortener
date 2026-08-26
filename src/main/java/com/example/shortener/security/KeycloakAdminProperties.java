package com.example.shortener.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.keycloak.admin")
public class KeycloakAdminProperties {

    private boolean enabled = false;
    private String serverUrl = "http://localhost:8081";
    private String realm = "shortener";
    private String clientId = "shortener-admin";
    private String clientSecret = "";
    private String inviteClientId = "shortener-dashboard";
    private String inviteRedirectUri = "http://localhost:3001/";
    private boolean sendInviteEmail = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getInviteClientId() {
        return inviteClientId;
    }

    public void setInviteClientId(String inviteClientId) {
        this.inviteClientId = inviteClientId;
    }

    public String getInviteRedirectUri() {
        return inviteRedirectUri;
    }

    public void setInviteRedirectUri(String inviteRedirectUri) {
        this.inviteRedirectUri = inviteRedirectUri;
    }

    public boolean isSendInviteEmail() {
        return sendInviteEmail;
    }

    public void setSendInviteEmail(boolean sendInviteEmail) {
        this.sendInviteEmail = sendInviteEmail;
    }
}
