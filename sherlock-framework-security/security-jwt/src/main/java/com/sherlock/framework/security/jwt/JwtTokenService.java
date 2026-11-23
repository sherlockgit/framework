package com.sherlock.framework.security.jwt;

import java.time.Duration;

public interface JwtTokenService {

    String issue(JwtClaims claims, Duration timeToLive);

    JwtClaims verify(String token);
}
