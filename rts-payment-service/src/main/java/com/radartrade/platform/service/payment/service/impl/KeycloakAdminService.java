package com.radartrade.platform.service.payment.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference; // THÊM IMPORT NÀY
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final WebClient webClient;
    private final String serverUrl;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final String vipRoleName;

    public KeycloakAdminService(WebClient.Builder webClientBuilder,
                                @Value("${keycloak.auth-server-url}") String serverUrl,
                                @Value("${keycloak.realm}") String realm,
                                @Value("${keycloak.admin.client-id}") String adminClientId,
                                @Value("${keycloak.admin.client-secret}") String adminClientSecret,
                                @Value("${keycloak.vip-role-name}") String vipRoleName) {
        this.serverUrl = serverUrl;
        this.webClient = webClientBuilder.baseUrl(this.serverUrl).build();
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.vipRoleName = vipRoleName;
        log.info("KeycloakAdminService initialized. Server URL: {}", this.serverUrl);
    }

    public Mono<Void> assignVipRoleToUser(UUID userId) {
        log.info("Attempting to assign VIP role to user {}", userId);
        return getAdminAccessToken()
                .doOnSuccess(token -> log.info("Successfully obtained Keycloak admin token."))
                .flatMap(accessToken -> findRoleByName(accessToken, vipRoleName)
                        .doOnSuccess(role -> log.info("Successfully found role '{}'.", vipRoleName))
                        .flatMap(role -> assignRoleToUser(accessToken, userId, role))
                )
                .doOnError(error -> log.error("Failed during VIP role assignment process for user {}: {}", userId, error.getMessage()))
                .then();
    }

    private Mono<String> getAdminAccessToken() {
        String tokenUrl = String.format("/realms/%s/protocol/openid-connect/token", realm);
        log.info("Requesting admin token from URL: {}{}", this.serverUrl, tokenUrl);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);

        return webClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                // ===== THAY ĐỔI Ở ĐÂY =====
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(response -> (String) response.get("access_token"));
    }

    private Mono<Map<String, Object>> findRoleByName(String accessToken, String roleName) {
        String rolesUrl = String.format("/admin/realms/%s/roles/%s", realm, roleName);
        log.info("Finding role '{}' from URL: {}{}", roleName, this.serverUrl, rolesUrl);
        return webClient.get()
                .uri(rolesUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                // ===== VÀ THAY ĐỔI Ở ĐÂY =====
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private Mono<Void> assignRoleToUser(String accessToken, UUID userId, Map<String, Object> role) {
        String roleMappingUrl = String.format("/admin/realms/%s/users/%s/role-mappings/realm", realm, userId);
        log.info("Assigning role to user {} via URL: {}{}", userId, this.serverUrl, roleMappingUrl);

        List<Map<String, Object>> rolesToAdd = List.of(role);

        return webClient.post()
                .uri(roleMappingUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rolesToAdd)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> log.info("Successfully received response from Keycloak for assigning role to user {}", userId))
                .then();
    }
}