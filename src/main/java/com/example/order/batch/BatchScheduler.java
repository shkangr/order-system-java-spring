package com.example.order.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that triggers batch jobs on a cron schedule.
 *
 *   - Coupon expiration:       every day at 00:00
 *   - Daily sales statistics:  every day at 02:00
 *   - Monthly settlement:      1st of each month at 03:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job couponExpirationJob;
    private final Job dailySalesStatisticsJob;
    private final Job monthlySettlementJob;

    @Scheduled(cron = "0 0 0 * * *")
    public void runCouponExpiration() {
        runJob(couponExpirationJob, "couponExpirationJob");
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailySalesStatistics() {
        runJob(dailySalesStatisticsJob, "dailySalesStatisticsJob");
    }

    @Scheduled(cron = "0 0 3 1 * *")
    public void runMonthlySettlement() {
        runJob(monthlySettlementJob, "monthlySettlementJob");
    }

    private void runJob(Job job, String jobName) {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[BatchScheduler] Starting: {}", jobName);
            jobLauncher.run(job, params);
            log.info("[BatchScheduler] Completed: {}", jobName);
        } catch (Exception e) {
            log.error("[BatchScheduler] Failed: {}", jobName, e);
        }
    }
}
