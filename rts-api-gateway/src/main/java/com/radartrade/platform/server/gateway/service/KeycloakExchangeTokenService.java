package com.radartrade.platform.server.gateway.service;

import com.radartrade.platform.server.gateway.dto.response.ResponseToken;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service("KeycloakExchangeTokenService")
public class KeycloakExchangeTokenService implements ExchangeTokenService {

    private RestClient restClient = RestClient.create();

    @Value("${token.exchange.provider.keycloak.endpoint}")
    private String tokenEndpoint;
    @Value("${token.exchange.provider.keycloak.client-secret}")
    private String CLIENT_SECRET;
    @Value("${token.exchange.provider.keycloak.client-id}")
    private String CLIENT_ID;
    @Value("${token.exchange.provider.keycloak.redirect-uri}")
    private String REDIRECT_URI;

    @PostConstruct
    public void onConnect() {
        restClient = RestClient.create(tokenEndpoint);
    }

    public ResponseToken exchangeToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", CLIENT_SECRET);
        body.add("redirect_uri", REDIRECT_URI);
        body.add("code", code);

        Map<String, String> tokenResponse = restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return buildReponseToken(tokenResponse);
    }

    @Override
    public ResponseToken refreshToken(String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("client_id", CLIENT_ID);
        body.add("client_secret", CLIENT_SECRET);
        body.add("refresh_token", refreshToken);

        Map<String, String> tokenResponse = restClient
                .post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return buildReponseToken(tokenResponse);
    }

    private ResponseToken buildReponseToken(Map<String, String> tokenResponse) {
        return new ResponseToken(
                tokenResponse.get("access_token"),
                tokenResponse.get("refresh_token"),
                tokenResponse.get("id_token")
        );
    }
}