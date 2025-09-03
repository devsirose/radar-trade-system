package com.radartrade.platform.service.backtest.service;

import com.radartrade.platform.service.backtest.dto.KlineValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.*;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StrategyService {

    @Value("${app.model-storage-path}")
    private String storagePath;

    private final Map<String, MultiLayerNetwork> modelCache = new ConcurrentHashMap<>();

    private MultiLayerNetwork loadModel(String modelFilename) throws Exception {
        if (modelCache.containsKey(modelFilename)) {
            System.out.println("Lấy model '" + modelFilename + "' từ cache.");
            return modelCache.get(modelFilename);
        }
        System.out.println("Tải model '" + modelFilename + "' từ file...");
        String fullPath = storagePath + File.separator + modelFilename;
        MultiLayerNetwork model = KerasModelImport.importKerasSequentialModelAndWeights(fullPath);
        modelCache.put(modelFilename, model);
        System.out.println("Model '" + modelFilename + "' đã được lưu vào cache.");
        return model;
    }

    private BarSeries createBarSeries(List<KlineValue> klines) {
        BarSeries series = new BaseBarSeriesBuilder().withName("series").build();
        for (KlineValue kline : klines) {
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(kline.getOpenTime(), ZoneId.of("UTC"));
            series.addBar(dateTime, kline.getOpen(), kline.getHigh(), kline.getLow(), kline.getClose(), kline.getVolume());
        }
        return series;
    }

    private TradingRecord runStrategyAndCloseOpenPosition(BarSeries series, BaseStrategy strategy) {
        BarSeriesManager seriesManager = new BarSeriesManager(series);
        TradingRecord tradingRecord = seriesManager.run(strategy);
        Position currentPosition = tradingRecord.getCurrentPosition();
        if (currentPosition.isOpened()) {
            int lastBarIndex = series.getEndIndex();
            Bar lastBar = series.getBar(lastBarIndex);
            tradingRecord.exit(lastBarIndex, lastBar.getClosePrice(), series.numOf(1));
        }
        return tradingRecord;
    }

    // --- BẮT ĐẦU PHẦN TỐI ƯU QUAN TRỌNG ---
    private List<Boolean> getAiPredictionsInBatch(MultiLayerNetwork model, BarSeries series) {
        final int historySize = 60;
        int seriesSize = series.getBarCount();
        if (seriesSize <= historySize) return new ArrayList<>();

        List<Boolean> predictions = new ArrayList<>();

        // Chuẩn bị batch rỗng
        INDArray batchInput = Nd4j.create(seriesSize - historySize, 1, historySize);

        for (int i = historySize; i < seriesSize; i++) {
            double[] windowData = new double[historySize];
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            // Tìm min/max trong cửa sổ 60 nến
            for (int j = 0; j < historySize; j++) {
                double price = series.getBar(i - historySize + j).getClosePrice().doubleValue();
                windowData[j] = price;
                if (price < min) min = price;
                if (price > max) max = price;
            }

            // Áp dụng MinMaxScaler: (value - min) / (max - min)
            double[] normalizedData = new double[historySize];
            double range = max - min;
            // Tránh chia cho 0 nếu tất cả giá trị bằng nhau
            if (range == 0) range = 1;

            for (int j = 0; j < historySize; j++) {
                normalizedData[j] = (windowData[j] - min) / range;
            }

            batchInput.putRow(i - historySize, Nd4j.create(normalizedData, 1, historySize));
        }

        // Thực hiện dự đoán trên toàn bộ batch đã được chuẩn hóa
        INDArray batchOutput = model.output(batchInput);

        for (int i = 0; i < historySize; i++) predictions.add(false); // Giai đoạn khởi động
        for (int i = 0; i < batchOutput.rows(); i++) {
            predictions.add(batchOutput.getDouble(i, 0) > 0.5);
        }

        return predictions;
    }
    // --- KẾT THÚC PHẦN TỐI ƯU QUAN TRỌNG ---

    public List<Map<String, Object>> runMaCrossoverStrategy(List<KlineValue> klines, int shortMa, int longMa) {
        BarSeries series = createBarSeries(klines);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortMa);
        SMAIndicator longSma = new SMAIndicator(closePrice, longMa);
        BaseStrategy strategy = new BaseStrategy(new CrossedUpIndicatorRule(shortSma, longSma), new CrossedDownIndicatorRule(shortSma, longMa));
        TradingRecord tradingRecord = runStrategyAndCloseOpenPosition(series, strategy);
        return formatTradingRecord(tradingRecord, series);
    }

    public List<Map<String, Object>> runMaCrossoverWithAI(List<KlineValue> klines, int shortMa, int longMa, String modelFilename) throws Exception {
        MultiLayerNetwork model = loadModel(modelFilename);
        BarSeries series = createBarSeries(klines);
        List<Boolean> aiPredictions = getAiPredictionsInBatch(model, series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortMa);
        SMAIndicator longSma = new SMAIndicator(closePrice, longMa);
        Rule entryRule = new CrossedUpIndicatorRule(shortSma, longSma).and(new AiPredictionRule(aiPredictions, true));
        Rule exitRule = new CrossedDownIndicatorRule(shortSma, longMa);
        BaseStrategy strategy = new BaseStrategy(entryRule, exitRule);
        TradingRecord tradingRecord = runStrategyAndCloseOpenPosition(series, strategy);
        return formatTradingRecord(tradingRecord, series);
    }

    public List<Map<String, Object>> runRsiStrategy(List<KlineValue> klines, int rsiPeriod, int entryThreshold, int exitThreshold) {
        BarSeries series = createBarSeries(klines);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);
        BaseStrategy strategy = new BaseStrategy(new UnderIndicatorRule(rsi, entryThreshold), new OverIndicatorRule(rsi, exitThreshold));
        TradingRecord tradingRecord = runStrategyAndCloseOpenPosition(series, strategy);
        return formatTradingRecord(tradingRecord, series);
    }

    public List<Map<String, Object>> runRsiStrategyWithAI(List<KlineValue> klines, int rsiPeriod, int entryThreshold, int exitThreshold, String modelFilename) throws Exception {
        MultiLayerNetwork model = loadModel(modelFilename);
        BarSeries series = createBarSeries(klines);
        List<Boolean> aiPredictions = getAiPredictionsInBatch(model, series);
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiPeriod);
        Rule entryRule = new UnderIndicatorRule(rsi, entryThreshold).and(new AiPredictionRule(aiPredictions, true));
        Rule exitRule = new OverIndicatorRule(rsi, exitThreshold);
        BaseStrategy strategy = new BaseStrategy(entryRule, exitRule);
        TradingRecord tradingRecord = runStrategyAndCloseOpenPosition(series, strategy);
        return formatTradingRecord(tradingRecord, series);
    }

    private static class AiPredictionRule extends AbstractRule {
        private final List<Boolean> predictions;
        private final boolean predictUp;
        public AiPredictionRule(List<Boolean> predictions, boolean predictUp) {
            this.predictions = predictions;
            this.predictUp = predictUp;
        }
        @Override
        public boolean isSatisfied(int index, TradingRecord tradingRecord) {
            if (index >= predictions.size()) return false;
            return predictions.get(index) == this.predictUp;
        }
    }

    private List<Map<String, Object>> formatTradingRecord(TradingRecord tradingRecord, BarSeries series) {
        List<Map<String, Object>> trades = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        for (Position position : tradingRecord.getPositions()) {
            Trade entry = position.getEntry();
            Trade exit = position.getExit();
            double profit = exit.getNetPrice().doubleValue() - entry.getNetPrice().doubleValue();
            double profitPercent = (profit / entry.getNetPrice().doubleValue()) * 100;
            Map<String, Object> tradeMap = new HashMap<>();
            tradeMap.put("entry_time", formatter.format(series.getBar(entry.getIndex()).getBeginTime().toInstant()));
            tradeMap.put("entry_price", entry.getNetPrice().doubleValue());
            tradeMap.put("exit_time", formatter.format(series.getBar(exit.getIndex()).getBeginTime().toInstant()));
            tradeMap.put("exit_price", exit.getNetPrice().doubleValue());
            tradeMap.put("profit_percent", profitPercent);
            tradeMap.put("result", profit > 0 ? "WIN" : "LOSS");
            trades.add(tradeMap);
        }
        return trades;
    }
}