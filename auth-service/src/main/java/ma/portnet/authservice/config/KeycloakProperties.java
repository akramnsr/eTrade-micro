package ma.portnet.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    private String authServerUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    public String getAuthServerUrl()  { return authServerUrl; }
    public void setAuthServerUrl(String v) { this.authServerUrl = v; }

    public String getRealm()          { return realm; }
    public void setRealm(String v)    { this.realm = v; }

    public String getClientId()       { return clientId; }
    public void setClientId(String v) { this.clientId = v; }

    public String getClientSecret()   { return clientSecret; }
    public void setClientSecret(String v) { this.clientSecret = v; }

    public String getTokenUrl() {
        return authServerUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";
    }

    public String getLogoutUrl() {
        return authServerUrl + "/realms/" + realm
                + "/protocol/openid-connect/logout";
    }
}