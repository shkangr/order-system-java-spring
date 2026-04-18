package com.example.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API for manually triggering batch jobs.
 * Requires ADMIN role (enforced by SecurityConfig: /api/admin/** → hasRole("ADMIN"))
 */
@Tag(name = "Admin - Batch", description = "Manually trigger batch jobs (ADMIN only)")
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
public class AdminBatchController {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @Operation(summary = "Run coupon expiration job")
    @PostMapping("/coupon-expiration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runCouponExpiration() {
        return launchJob("couponExpirationJob", new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters());
    }

    @Operation(summary = "Run daily sales statistics job")
    @PostMapping("/daily-sales-statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runDailySalesStatistics() {
        return launchJob("dailySalesStatisticsJob", new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters());
    }

    @Operation(summary = "Run monthly settlement job",
            description = "targetMonth format: yyyy-MM (default: previous month)")
    @PostMapping("/monthly-settlement")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> runMonthlySettlement(
            @RequestParam(required = false) String targetMonth) {

        JobParametersBuilder builder = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis());
        if (targetMonth != null && !targetMonth.isBlank()) {
            builder.addString("targetMonth", targetMonth);
        }

        return launchJob("monthlySettlementJob", builder.toJobParameters());
    }

    private ResponseEntity<Map<String, String>> launchJob(String jobName, JobParameters params) {
        try {
            Job job = applicationContext.getBean(jobName, Job.class);
            var execution = jobLauncher.run(job, params);

            log.info("[AdminBatch] Launched: {}, status: {}", jobName, execution.getStatus());
            return ResponseEntity.ok(Map.of(
                    "job", jobName,
                    "status", execution.getStatus().toString(),
                    "executionId", String.valueOf(execution.getId())
            ));
        } catch (Exception e) {
            log.error("[AdminBatch] Failed: {}", jobName, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "job", jobName,
                    "status", "FAILED",
                    "error", e.getMessage()
            ));
        }
    }
}
