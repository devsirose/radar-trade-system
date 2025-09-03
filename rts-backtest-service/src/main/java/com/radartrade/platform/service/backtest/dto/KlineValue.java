package com.radartrade.platform.service.backtest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.Instant;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KlineValue {
    private Instant openTime;
    private String open;
    private String high;
    private String low;
    private String close;
    private String volume;
}