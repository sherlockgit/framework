package com.sherlock.framework.security.autoconfigure;

import com.sherlock.framework.security.web.PrincipalContextFilter;
import com.sherlock.framework.security.web.StrictJwtValidator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

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
        decoder.setJwtValidator(new StrictJwtValidator(properties.getIssuer(), properties.getAudience()));
        return decoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "sherlock.security.jwt", name = {"issuer", "audience"})
    StrictJwtValidator strictJwtValidator(SecurityJwtProperties properties) {
        properties.requireIssuerAndAudience();
        return new StrictJwtValidator(properties.getIssuer(), properties.getAudience());
    }

    @Bean
    @Conditional(JwtPropertiesConfiguredCondition.class)
    SecurityJwtPropertiesValidator securityJwtPropertiesValidator(SecurityJwtProperties properties) {
        properties.requireIssuerAndAudience();
        return new SecurityJwtPropertiesValidator();
    }

    @Bean
    static BeanPostProcessor jwtDecoderValidatorConfigurer(ObjectProvider<SecurityJwtProperties> propertiesProvider) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof NimbusJwtDecoder decoder) {
                    SecurityJwtProperties properties = propertiesProvider.getIfAvailable();
                    if (properties != null && properties.hasIssuerAndAudience()) {
                        decoder.setJwtValidator(new StrictJwtValidator(properties.getIssuer(), properties.getAudience()));
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
    PrincipalContextFilter principalContextFilter(ObjectProvider<StrictJwtValidator> jwtValidatorProvider) {
        return new PrincipalContextFilter(jwtValidatorProvider.getIfAvailable());
    }

    private static final class SecurityJwtPropertiesValidator {
    }

    private static final class JwtPropertiesConfiguredCondition extends AnyNestedCondition {

        JwtPropertiesConfiguredCondition() {
            super(ConfigurationCondition.ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(prefix = "sherlock.security.jwt", name = "issuer")
        static final class IssuerConfigured {
        }

        @ConditionalOnProperty(prefix = "sherlock.security.jwt", name = "audience")
        static final class AudienceConfigured {
        }

        @ConditionalOnProperty(prefix = "sherlock.security.jwt", name = "jwk-set-uri")
        static final class JwkSetUriConfigured {
        }

    }
}
