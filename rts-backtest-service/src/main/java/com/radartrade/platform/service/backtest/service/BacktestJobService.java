package com.radartrade.platform.service.backtest.service;

import com.radartrade.platform.service.backtest.dto.BacktestRequest;
import com.radartrade.platform.service.backtest.dto.BacktestResult;
import com.radartrade.platform.service.backtest.dto.KlineValue;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BacktestJobService {

    private final DataService dataService;
    private final StrategyService strategyService;
    private final Map<String, CompletableFuture<BacktestResult>> jobs = new ConcurrentHashMap<>();

    public BacktestJobService(DataService dataService, StrategyService strategyService) {
        this.dataService = dataService;
        this.strategyService = strategyService;
    }

    public Map<String, Object> getJobStatus(String jobId) {
        CompletableFuture<BacktestResult> job = jobs.get(jobId);
        if (job == null) {
            return Map.of("status", "NOT_FOUND");
        }
        if (job.isDone()) {
            if(job.isCompletedExceptionally()){
                return Map.of("status", "FAILED");
            }
            return Map.of("status", "COMPLETED");
        }
        return Map.of("status", "RUNNING");
    }

    public BacktestResult getJobResult(String jobId) {
        CompletableFuture<BacktestResult> job = jobs.get(jobId);
        if (job == null || !job.isDone()) {
            return null;
        }
        try {
            return job.get();
        } catch (Exception e) {
            return BacktestResult.builder()
                    .summary(Map.of("error", e.getMessage()))
                    .trades(List.of())
                    .build();
        }
    }

    @Async
    public void startBacktestJob(String jobId, BacktestRequest request, String token) {
        CompletableFuture<BacktestResult> future = new CompletableFuture<>();
        jobs.put(jobId, future);
        String modelFilename = request.getModelFilename();
        boolean useAi = modelFilename != null && !modelFilename.isEmpty();

        try {
            // 1. Get Data
            List<KlineValue> klines = dataService.getHistoricalData(
                    request.getSymbol(), request.getInterval(), request.getStartDate(), request.getEndDate(), token
            );


            // 2. Run Strategy
            Map<String, Object> strategyParams = request.getStrategy();
            String strategyName = (String) strategyParams.get("name");
            List<Map<String, Object>> trades;

            switch (strategyName) {
                case "ma_crossover":
                case "ma_crossover_ai": // Gộp cả hai case lại
                    int shortMa = (int) strategyParams.getOrDefault("ma_short", 30);
                    int longMa = (int) strategyParams.getOrDefault("ma_long", 90);

                    if (useAi) {
                        trades = strategyService.runMaCrossoverWithAI(klines, shortMa, longMa, modelFilename);
                    } else {
                        trades = strategyService.runMaCrossoverStrategy(klines, shortMa, longMa);
                    }
                    break;

                case "rsi_strategy":
                case "rsi_strategy_ai": // Gộp cả hai case lại
                    int rsiPeriod = (int) strategyParams.getOrDefault("rsi_period", 14);
                    int entryThreshold = (int) strategyParams.getOrDefault("rsi_entry", 30);
                    int exitThreshold = (int) strategyParams.getOrDefault("rsi_exit", 70);

                    if (useAi) {
                        // Bạn cần tạo phương thức runRsiStrategyWithAI trong StrategyService
                        trades = strategyService.runRsiStrategyWithAI(klines, rsiPeriod, entryThreshold, exitThreshold, modelFilename);
                    } else {
                        trades = strategyService.runRsiStrategy(klines, rsiPeriod, entryThreshold, exitThreshold);
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unsupported strategy: " + strategyName);
            }

            // 3. Calculate Summary
            Map<String, Object> summary = calculateSummary(trades);

            BacktestResult result = BacktestResult.builder().summary(summary).trades(trades).build();
            future.complete(result);

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    private Map<String, Object> calculateSummary(List<Map<String, Object>> trades) {
        if (trades.isEmpty()) {
            return Map.of("message", "No trades executed.");
        }
        int totalTrades = trades.size();
        long winningTrades = trades.stream().filter(t -> (double) t.get("profit_percent") > 0).count();
        double winRate = (double) winningTrades / totalTrades * 100;
        double totalProfitPercent = trades.stream().mapToDouble(t -> (double) t.get("profit_percent")).sum();

        Map<String, Object> summary = new HashMap<>();
        summary.put("total_trades", totalTrades);
        summary.put("winning_trades", winningTrades);
        summary.put("losing_trades", totalTrades - winningTrades);
        summary.put("win_rate_percent", winRate);
        summary.put("total_profit_percent", totalProfitPercent);
        return summary;
    }
}