package com.sherlock.framework.security.autoconfigure;

import com.sherlock.framework.security.web.PrincipalContextFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.math.BigDecimal;
import java.time.Clock;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityJwtProperties.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(prefix = "sherlock.security.jwt", name = "jwk-set-uri")
    JwtDecoder jwtDecoder(SecurityJwtProperties properties) {
        properties.requireIssuerAndAudience();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri().toString()).build();
        decoder.setJwtValidator(jwtValidator(properties));
        return decoder;
    }

    @Bean
    static BeanPostProcessor jwtDecoderValidatorConfigurer(ObjectProvider<SecurityJwtProperties> propertiesProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof NimbusJwtDecoder decoder) {
                    SecurityJwtProperties properties = propertiesProvider.getIfAvailable();
                    if (properties != null && properties.hasIssuerAndAudience()) {
                        decoder.setJwtValidator(jwtValidator(properties));
                    }
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("permissions");
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    @ConditionalOnMissingBean
    PrincipalContextFilter principalContextFilter() {
        return new PrincipalContextFilter();
    }

    private static OAuth2TokenValidator<Jwt> jwtValidator(SecurityJwtProperties properties) {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(properties.getClockSkew());
        timestampValidator.setClock(Clock.systemUTC());
        return new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(properties.getIssuer()),
                audienceValidator(properties.getAudience()),
                principalClaimsValidator());
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String audience) {
        return jwt -> jwt.getAudience().contains(audience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "JWT audience is invalid", null));
    }

    private static OAuth2TokenValidator<Jwt> principalClaimsValidator() {
        return jwt -> {
            try {
                positiveLong(jwt.getSubject(), "sub");
                positiveLongClaim(jwt, "tenant_id");
                nonNegativeLongClaim(jwt, "policy_version");
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException exception) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "JWT principal claims are invalid", null));
            }
        };
    }

    private static long positiveLongClaim(Jwt jwt, String claimName) {
        long value = longClaim(jwt, claimName);
        if (value <= 0) {
            throw new IllegalArgumentException(claimName + " must be positive");
        }
        return value;
    }

    private static long nonNegativeLongClaim(Jwt jwt, String claimName) {
        long value = longClaim(jwt, claimName);
        if (value < 0) {
            throw new IllegalArgumentException(claimName + " must not be negative");
        }
        return value;
    }

    private static long positiveLong(String value, String claimName) {
        long parsed = Long.parseLong(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException(claimName + " must be positive");
        }
        return parsed;
    }

    private static long longClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);
        if (value instanceof Number number) {
            try {
                return new BigDecimal(number.toString()).longValueExact();
            } catch (ArithmeticException | NumberFormatException exception) {
                throw new IllegalArgumentException(claimName + " must be an integer", exception);
            }
        }
        if (value instanceof String text) {
            return Long.parseLong(text);
        }
        throw new IllegalArgumentException(claimName + " must be a number");
    }
}
