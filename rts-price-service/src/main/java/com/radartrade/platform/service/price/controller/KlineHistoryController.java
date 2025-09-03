package com.radartrade.platform.service.price.controller;

import com.radartrade.platform.service.price.domain.KlineUpdate;
import com.radartrade.platform.service.price.service.impl.KlineHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/price/kline")
public class KlineHistoryController {

    private final KlineHistoryService klineHistoryService;

    public KlineHistoryController(KlineHistoryService klineHistoryService) {
        this.klineHistoryService = klineHistoryService;
    }

    /**
     * Endpoint này trả về một danh sách (JSON array) chứa dữ liệu kline lịch sử.
     */
    @GetMapping("/history")
    public ResponseEntity<List<KlineUpdate>> getKlineHistory(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(defaultValue = "1000") Integer limit) {

        List<KlineUpdate> history = klineHistoryService.getHistoricalKlines(symbol, interval, startTime, endTime, limit);
        return ResponseEntity.ok(history);
    }
}