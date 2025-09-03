package com.radartrade.platform.service.price.config;

import com.radartrade.platform.server.gateway.config.KeycloakRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // Tắt CSRF
                .authorizeExchange(exchange -> exchange
                        // Cho phép truy cập public
                        .pathMatchers("/api/v1/price/kline/stream/**").permitAll()
                        // Chỉ user có role "vip" mới được truy cập history
                        .pathMatchers("/api/v1/price/kline/history/**").hasRole("vip")
                        // Các endpoint khác cần xác thực
                        .anyExchange().authenticated()
                )
                // Cấu hình Resource Server sử dụng JWT + custom converter
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor()))
                );
        return http.build();
    }

    /**
     * Converter để lấy roles từ Keycloak ("realm_access.roles")
     * và map thành GrantedAuthority dạng ROLE_xxx.
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }
}
