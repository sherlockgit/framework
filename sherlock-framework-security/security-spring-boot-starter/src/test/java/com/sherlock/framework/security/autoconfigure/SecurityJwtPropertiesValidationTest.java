package com.sherlock.framework.security.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityJwtPropertiesValidationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withUserConfiguration(SecurityAutoConfiguration.class);

    @Test
    void issuerWithoutAudienceFailsFast() {
        contextRunner.withPropertyValues("sherlock.security.jwt.issuer=framework-issuer")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("issuer and audience are required"));
    }

    @Test
    void audienceWithoutIssuerFailsFast() {
        contextRunner.withPropertyValues("sherlock.security.jwt.audience=framework-api")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasMessageContaining("issuer and audience are required"));
    }
}
