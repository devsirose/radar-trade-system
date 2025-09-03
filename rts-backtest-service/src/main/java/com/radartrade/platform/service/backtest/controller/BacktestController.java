package com.radartrade.platform.service.backtest.controller;

import com.radartrade.platform.service.backtest.dto.BacktestRequest;
import com.radartrade.platform.service.backtest.dto.BacktestResult;
import com.radartrade.platform.service.backtest.dto.JobStatusResponse;
import com.radartrade.platform.service.backtest.service.BacktestJobService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backtest")
public class BacktestController {

    private final BacktestJobService backtestJobService;

    public BacktestController(BacktestJobService backtestJobService) {
        this.backtestJobService = backtestJobService;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runBacktest(
            @RequestBody BacktestRequest request,
            @RequestHeader("Authorization") String token) {
        String jobId = UUID.randomUUID().toString();
        backtestJobService.startBacktestJob(jobId, request, token);
        return ResponseEntity.accepted().body(Map.of("jobId", jobId));
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable String jobId) {
        Map<String, Object> statusInfo = backtestJobService.getJobStatus(jobId);
        String status = (String) statusInfo.get("status");

        if ("NOT_FOUND".equals(status)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JobStatusResponse(jobId, "NOT_FOUND", "Job with the given ID was not found."));
        }

        return ResponseEntity.ok(new JobStatusResponse(jobId, status, "Job status retrieved successfully."));
    }

    @GetMapping("/results/{jobId}")
    public ResponseEntity<?> getJobResults(@PathVariable String jobId) {
        Map<String, Object> statusInfo = backtestJobService.getJobStatus(jobId);
        String status = (String) statusInfo.get("status");

        if ("NOT_FOUND".equals(status)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new JobStatusResponse(jobId, "NOT_FOUND", "Job with the given ID was not found."));
        }

        if (!"COMPLETED".equals(status)) {
            return ResponseEntity.accepted()
                    .body(new JobStatusResponse(jobId, status, "Job is not yet completed."));
        }

        BacktestResult result = backtestJobService.getJobResult(jobId);
        return ResponseEntity.ok(result);
    }
}