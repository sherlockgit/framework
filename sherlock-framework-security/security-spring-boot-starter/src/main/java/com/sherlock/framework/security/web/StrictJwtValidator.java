package com.sherlock.framework.security.web;

import com.sherlock.framework.security.AuthenticatedPrincipal;
import com.sherlock.framework.tenant.TenantId;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 独立于具体解码器实现，校验框架定义的 JWT 契约。
 */
public final class StrictJwtValidator implements OAuth2TokenValidator<Jwt> {

    private static final String INVALID_TOKEN = "invalid_token";

    private final String issuer;
    private final String audience;
    private final Clock clock;

    public StrictJwtValidator(String issuer, String audience) {
        this(issuer, audience, Clock.systemUTC());
    }

    StrictJwtValidator(String issuer, String audience, Clock clock) {
        this.issuer = requiredText(issuer, "issuer");
        this.audience = requiredText(audience, "audience");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            validateClaims(jwt);
            return OAuth2TokenValidatorResult.success();
        } catch (RuntimeException exception) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(INVALID_TOKEN, "JWT claims are invalid", null));
        }
    }

    public AuthenticatedPrincipal validateAndCreatePrincipal(Jwt jwt) {
        validateClaims(jwt);
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        return new AuthenticatedPrincipal(
                positiveLong(jwt.getSubject(), "sub"),
                new TenantId(positiveLongClaim(jwt, "tenant_id")),
                permissions == null ? Set.of() : Set.copyOf(permissions),
                nonNegativeLongClaim(jwt, "policy_version"));
    }

    private void validateClaims(Jwt jwt) {
        Objects.requireNonNull(jwt, "jwt must not be null");
        if (!issuer.equals(jwt.getClaimAsString("iss"))) {
            throw new IllegalArgumentException("JWT issuer is invalid");
        }
        if (!jwt.getAudience().contains(audience)) {
            throw new IllegalArgumentException("JWT audience is invalid");
        }

        Instant now = clock.instant();
        Instant expiration = jwt.getExpiresAt();
        if (expiration == null || !expiration.isAfter(now)) {
            throw new IllegalArgumentException("JWT is expired");
        }
        Instant notBefore = jwt.getNotBefore();
        if (notBefore != null && notBefore.isAfter(now)) {
            throw new IllegalArgumentException("JWT is not active yet");
        }

        positiveLong(jwt.getSubject(), "sub");
        positiveLongClaim(jwt, "tenant_id");
        nonNegativeLongClaim(jwt, "policy_version");
        requiredText(jwt.getId(), "jti");
    }

    private static long positiveLongClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        long parsed = exactLong(value, claimName);
        if (parsed <= 0) {
            throw new IllegalArgumentException(claimName + " must be positive");
        }
        return parsed;
    }

    private static long nonNegativeLongClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        long parsed = exactLong(value, claimName);
        if (parsed < 0) {
            throw new IllegalArgumentException(claimName + " must not be negative");
        }
        return parsed;
    }

    private static long positiveLong(String value, String claimName) {
        if (value == null || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException(claimName + " must be a positive integer");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(claimName + " is out of range", exception);
        }
    }

    private static long exactLong(Object value, String claimName) {
        if (value instanceof Number number) {
            try {
                BigDecimal decimal = new BigDecimal(number.toString());
                if (decimal.scale() > 0) {
                    throw new IllegalArgumentException(claimName + " must not contain a decimal");
                }
                return decimal.longValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new IllegalArgumentException(claimName + " must be an integer", exception);
            }
        }
        if (value instanceof String text) {
            return integerLong(text, claimName);
        }
        throw new IllegalArgumentException(claimName + " must be an integer");
    }

    private static long integerLong(String value, String claimName) {
        if (value == null || !value.matches("-?(?:0|[1-9][0-9]*)")) {
            throw new IllegalArgumentException(claimName + " must be an integer");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(claimName + " is out of range", exception);
        }
    }

    private static String requiredText(String value, String claimName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(claimName + " must not be blank");
        }
        return value;
    }
}
