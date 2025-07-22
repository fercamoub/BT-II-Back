package com.example.bt2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for REST API
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/auth/**").permitAll() // Allow auth endpoints
                        .requestMatchers("/h2-console/**").permitAll() // Allow H2 console
                        .requestMatchers("/callback").permitAll() // Allow callback
                        .requestMatchers("/").permitAll() // Allow root
                        .requestMatchers("/error").permitAll() // Allow error page
                        .anyRequest().authenticated() // Secure other endpoints
                )
                .headers(headers -> headers.frameOptions().disable() // Allow H2 console to work
                )
                .oauth2Login(AbstractHttpConfigurer::disable) // Disable default OAuth2 login
                .formLogin(AbstractHttpConfigurer::disable) // Disable form login
                .httpBasic(AbstractHttpConfigurer::disable); // Disable basic auth

        return http.build();
    }
}