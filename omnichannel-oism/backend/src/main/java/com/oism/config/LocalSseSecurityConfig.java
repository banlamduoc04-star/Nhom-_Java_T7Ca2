package com.oism.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local")
public class LocalSseSecurityConfig {

    @Bean
    public SecurityFilterChain localSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/events/orders",
                    "/api/v1/events/test/orders/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(httpBasic -> {});

        return http.build();
    }
}
