
package com.radartrade.platform.service.backtest.service;

import com.radartrade.platform.service.backtest.dto.KlineValue;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class DataService {

    private final RestTemplate restTemplate;

    @Value("${app.price-service-url}")
    private String priceServiceUrl;

    public DataService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<KlineValue> getHistoricalData(String symbol, String interval, String startDate, String endDate, String token) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        long startTime = LocalDate.parse(startDate, formatter).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        long endTime = LocalDate.parse(endDate, formatter).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

        String url = UriComponentsBuilder.fromHttpUrl(priceServiceUrl + "/kline/history")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
                .queryParam("startTime", startTime)
                .queryParam("endTime", endTime)
                .queryParam("limit", 1500) // Tăng giới hạn để lấy đủ dữ liệu
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token); // Gắn token vào header

        // 2. Tạo HttpEntity chứa headers
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 3. Dùng restTemplate.exchange để gửi request với headers
        ResponseEntity<KlineValue[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                KlineValue[].class
        );

        KlineValue[] klines = response.getBody();
        return klines != null ? Arrays.asList(klines) : List.of();
    }
}