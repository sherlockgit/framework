package com.sherlock.framework.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class WebAutoConfigurationTest {

    @Test
    void errorHandlingAutoConfigurationRequiresServletWebApplication() throws Exception {
        Class<?> autoConfiguration = Class.forName("com.sherlock.framework.web.autoconfigure.WebAutoConfiguration");
        AutoConfigurations configurations = AutoConfigurations.of(autoConfiguration);

        new ApplicationContextRunner()
                .withConfiguration(configurations)
                .run(context -> assertThat(context).doesNotHaveBean("frameworkErrorHandler"));

        new WebApplicationContextRunner()
                .withConfiguration(configurations)
                .run(context -> assertThat(context).hasBean("frameworkErrorHandler"));
    }
}
