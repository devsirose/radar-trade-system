// src/main/java/com/radartrade/platform/service/backtest/dto/BacktestResult.java
package com.radartrade.platform.service.backtest.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class BacktestResult {
    private Map<String, Object> summary;
    private List<Map<String, Object>> trades;
}