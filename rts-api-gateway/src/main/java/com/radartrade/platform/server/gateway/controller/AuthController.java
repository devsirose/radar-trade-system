package com.radartrade.platform.server.gateway.controller;

import com.radartrade.platform.server.gateway.dto.response.ResponseToken;
import com.radartrade.platform.server.gateway.service.ExchangeTokenService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("auth")
public class AuthController {
    private final ExchangeTokenService exchangeTokenService;

    public AuthController(@Qualifier("KeycloakExchangeTokenService") ExchangeTokenService exchangeTokenService) {
        this.exchangeTokenService = exchangeTokenService;
    }


    @PostMapping(
            value = "/refresh_token",
            consumes = { MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.ALL_VALUE },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ResponseToken>> refreshToken(
            @RequestParam(value = "refreshToken", required = false) String refreshToken,
            ServerWebExchange exchange) {

        Mono<String> tokenMono = Mono.justOrEmpty(refreshToken)
                .switchIfEmpty(exchange.getFormData().map(m -> m.getFirst("refreshToken")));

        return tokenMono
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Missing refreshToken")))
                .flatMap(rt -> Mono.fromCallable(() -> exchangeTokenService.refreshToken(rt))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(ResponseEntity::ok);
    }
}