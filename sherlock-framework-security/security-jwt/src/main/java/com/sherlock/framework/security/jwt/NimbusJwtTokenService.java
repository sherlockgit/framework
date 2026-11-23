package com.sherlock.framework.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sherlock.framework.tenant.TenantId;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class NimbusJwtTokenService implements JwtTokenService {

    private static final String TENANT_ID_CLAIM = "tenant_id";
    private static final String PERMISSIONS_CLAIM = "permissions";
    private static final String POLICY_VERSION_CLAIM = "policy_version";
    private static final Duration CLOCK_SKEW = Duration.ZERO;

    private final RSAPrivateKey signingKey;
    private final RSAPublicKey verificationKey;
    private final String issuer;
    private final String audience;
    private final Clock clock;

    public NimbusJwtTokenService(
            RSAPrivateKey signingKey,
            RSAPublicKey verificationKey,
            String issuer,
            String audience,
            Clock clock) {
        this.signingKey = Objects.requireNonNull(signingKey, "signingKey must not be null");
        this.verificationKey = Objects.requireNonNull(verificationKey, "verificationKey must not be null");
        this.issuer = requiredText(issuer, "issuer");
        this.audience = requiredText(audience, "audience");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public String issue(JwtClaims claims, Duration timeToLive) {
        Objects.requireNonNull(claims, "claims must not be null");
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("timeToLive must be positive");
        }

        Instant now = clock.instant();
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(Long.toString(claims.userId()))
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(timeToLive)))
                .jwtID(claims.tokenId())
                .claim(TENANT_ID_CLAIM, claims.tenantId().value())
                .claim(PERMISSIONS_CLAIM, List.copyOf(claims.permissions()))
                .claim(POLICY_VERSION_CLAIM, claims.policyVersion())
                .build();
        SignedJWT signedJwt = new SignedJWT(
                new com.nimbusds.jose.JWSHeader.Builder(JWSAlgorithm.RS256).build(), claimsSet);
        try {
            signedJwt.sign(new RSASSASigner(signingKey));
            return signedJwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign JWT", exception);
        }
    }

    @Override
    public JwtClaims verify(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("JWT must not be blank");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())
                    || !signedJwt.verify(new RSASSAVerifier(verificationKey))) {
                throw new SecurityException("JWT signature is invalid");
            }

            JWTClaimsSet claims = signedJwt.getJWTClaimsSet();
            validateRegisteredClaims(claims);
            return new JwtClaims(
                    Long.parseLong(claims.getSubject()),
                    new TenantId(claims.getLongClaim(TENANT_ID_CLAIM)),
                    Set.copyOf(claims.getStringListClaim(PERMISSIONS_CLAIM)),
                    claims.getLongClaim(POLICY_VERSION_CLAIM),
                    claims.getJWTID());
        } catch (ParseException | JOSEException | IllegalArgumentException | NullPointerException exception) {
            throw new SecurityException("JWT is invalid", exception);
        }
    }

    private void validateRegisteredClaims(JWTClaimsSet claims) {
        if (!issuer.equals(claims.getIssuer())) {
            throw new SecurityException("JWT issuer is invalid");
        }
        if (!claims.getAudience().contains(audience)) {
            throw new SecurityException("JWT audience is invalid");
        }
        Date expirationTime = claims.getExpirationTime();
        Instant now = clock.instant();
        if (expirationTime == null || !expirationTime.toInstant().plus(CLOCK_SKEW).isAfter(now)) {
            throw new SecurityException("JWT is expired");
        }
        Date notBeforeTime = claims.getNotBeforeTime();
        if (notBeforeTime != null && notBeforeTime.toInstant().minus(CLOCK_SKEW).isAfter(now)) {
            throw new SecurityException("JWT is not active yet");
        }
    }

    private static String requiredText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
