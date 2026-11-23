package com.sherlock.framework.example;

import com.sherlock.framework.data.scope.DataScope;
import com.sherlock.framework.data.scope.DataScopeContext;
import com.sherlock.framework.security.web.PrincipalContextFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

@SpringBootApplication
@MapperScan("com.sherlock.framework.example.order")
public class TenantDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantDemoApplication.class, args);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PrincipalContextFilter principalContextFilter,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/orders").hasAuthority("order:write")
                        .requestMatchers("/orders/**").hasAuthority("order:read")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterAfter(principalContextFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new SelfDataScopeFilter(), PrincipalContextFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<PrincipalContextFilter> principalContextFilterRegistration(
            PrincipalContextFilter principalContextFilter) {
        FilterRegistrationBean<PrincipalContextFilter> registration = new FilterRegistrationBean<>(principalContextFilter);
        registration.setEnabled(false);
        return registration;
    }

    private static final class SelfDataScopeFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, java.io.IOException {
            DataScopeContext.set(DataScope.self());
            try {
                filterChain.doFilter(request, response);
            } finally {
                DataScopeContext.clear();
            }
        }
    }
}
