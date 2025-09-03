
package com.radartrade.platform.service.backtest.dto;

import lombok.Data;
import java.util.Map;

@Data
public class BacktestRequest {
    private String symbol;
    private String interval;
    private String startDate;
    private String endDate;
    private Map<String, Object> strategy;
    private String modelFilename;
}