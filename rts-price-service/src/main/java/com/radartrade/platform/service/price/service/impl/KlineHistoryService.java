package com.radartrade.platform.service.price.service.impl;

import com.radartrade.platform.service.price.domain.KlineUpdate;
import com.radartrade.platform.service.price.service.client.KlineRestConsumer;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KlineHistoryService {

    private final KlineRestConsumer klineRestConsumer;

    public KlineHistoryService(KlineRestConsumer klineRestConsumer) {
        this.klineRestConsumer = klineRestConsumer;
    }

    /**
     * Lấy dữ liệu kline lịch sử bằng cách gọi trực tiếp đến API của sàn (ví dụ: Binance).
     *
     * @param symbol    Mã giao dịch (ví dụ: "BTCUSDT")
     * @param interval  Khung thời gian (ví dụ: "1d")
     * @param startTime Thời gian bắt đầu (timestamp milliseconds)
     * @param endTime   Thời gian kết thúc (timestamp milliseconds)
     * @param limit     Số lượng nến tối đa
     * @return Một List chứa các object KlineUpdate
     */
    public List<KlineUpdate> getHistoricalKlines(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        // Tận dụng lại consumer đã có để gọi đến API của sàn
        return klineRestConsumer.getHistoricalKlines(symbol, interval, startTime, endTime, limit);
    }
}