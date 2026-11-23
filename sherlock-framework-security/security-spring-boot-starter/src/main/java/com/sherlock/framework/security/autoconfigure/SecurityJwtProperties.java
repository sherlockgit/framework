package com.sherlock.framework.security.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;

@ConfigurationProperties("sherlock.security.jwt")
public class SecurityJwtProperties {

    private String issuer;
    private String audience;
    private URI jwkSetUri;
    private Duration clockSkew = Duration.ZERO;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public URI getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(URI jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public Duration getClockSkew() {
        return clockSkew;
    }

    public void setClockSkew(Duration clockSkew) {
        this.clockSkew = clockSkew == null ? Duration.ZERO : clockSkew;
    }

    boolean hasIssuerAndAudience() {
        return hasText(issuer) && hasText(audience);
    }

    void requireIssuerAndAudience() {
        if (!hasIssuerAndAudience()) {
            throw new IllegalStateException("sherlock.security.jwt.issuer and audience are required when JWT is configured");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
