// java/com/radartrade/platform/server/gateway/controller/CallbackController.java

package com.radartrade.platform.server.gateway.controller;

import com.radartrade.platform.server.gateway.dto.response.ResponseToken;
import com.radartrade.platform.server.gateway.service.ExchangeTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
public class CallbackController {

    private final ExchangeTokenService exchangeTokenService;

    public CallbackController(@Qualifier("KeycloakExchangeTokenService") ExchangeTokenService exchangeTokenService) {
        this.exchangeTokenService = exchangeTokenService;
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam(required = false) String state) {
        ResponseToken tokens = exchangeTokenService.exchangeToken(code);
        URI redirectUri = UriComponentsBuilder.fromUriString("http://localhost:5173/auth/receive-token")
                .queryParam("token", tokens.token())
                .queryParam("refreshToken", tokens.refreshToken())
                .queryParam("idToken", tokens.idToken())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(redirectUri);


        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}