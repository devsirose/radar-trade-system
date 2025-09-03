package com.radartrade.platform.service.backtest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Tắt CSRF vì đây là API service
                .csrf(csrf -> csrf.disable())
                // Cấu hình các quy tắc cho request
                .authorizeHttpRequests(authorize -> authorize
                        // Yêu cầu tất cả các request đến /api/v1/backtest/** phải được xác thực
                        .requestMatchers("/api/v1/backtest/**").authenticated()
                        // Cho phép tất cả các request khác (nếu có)
                        .anyRequest().permitAll()
                )
                // Cấu hình service này là một OAuth2 Resource Server sử dụng JWT
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}