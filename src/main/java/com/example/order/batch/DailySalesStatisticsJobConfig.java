package com.example.order.batch;

import com.example.order.domain.DailySalesStatistics;
import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.repository.DailySalesStatisticsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Batch Job: aggregates yesterday's order data into DailySalesStatistics.
 *
 * Tasklet-based (single aggregation, not item-by-item).
 * Scheduled: every day at 2am
 */
@Configuration
public class DailySalesStatisticsJobConfig {

    private static final Logger log = LoggerFactory.getLogger(DailySalesStatisticsJobConfig.class);

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final DailySalesStatisticsRepository dailySalesStatisticsRepository;

    public DailySalesStatisticsJobConfig(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         EntityManagerFactory entityManagerFactory,
                                         DailySalesStatisticsRepository dailySalesStatisticsRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.dailySalesStatisticsRepository = dailySalesStatisticsRepository;
    }

    @Bean
    public Job dailySalesStatisticsJob() {
        return new JobBuilder("dailySalesStatisticsJob", jobRepository)
                .start(dailySalesStatisticsStep())
                .build();
    }

    @Bean
    public Step dailySalesStatisticsStep() {
        return new StepBuilder("dailySalesStatisticsStep", jobRepository)
                .tasklet(dailySalesStatisticsTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet dailySalesStatisticsTasklet() {
        return (contribution, chunkContext) -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDateTime startOfYesterday = yesterday.atStartOfDay();
            LocalDateTime startOfToday = yesterday.plusDays(1).atStartOfDay();

            if (dailySalesStatisticsRepository.existsBySalesDate(yesterday)) {
                log.info("[DailySalesBatch] Statistics already exist for {}. Skipping.", yesterday);
                return RepeatStatus.FINISHED;
            }

            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                List<Order> orders = em.createQuery(
                        "SELECT o FROM Order o JOIN FETCH o.orderItems " +
                        "WHERE o.orderDate >= :start AND o.orderDate < :end",
                        Order.class)
                        .setParameter("start", startOfYesterday)
                        .setParameter("end", startOfToday)
                        .getResultList();

                long totalOrderCount = orders.stream()
                        .filter(o -> o.getStatus() == OrderStatus.ORDER).count();
                long totalRevenue = orders.stream()
                        .filter(o -> o.getStatus() == OrderStatus.ORDER)
                        .mapToLong(Order::getTotalPrice).sum();
                long totalDiscountAmount = orders.stream()
                        .filter(o -> o.getStatus() == OrderStatus.ORDER)
                        .mapToLong(Order::getDiscountAmount).sum();
                long averageOrderPrice = totalOrderCount > 0 ? totalRevenue / totalOrderCount : 0;
                long cancelledOrderCount = orders.stream()
                        .filter(o -> o.getStatus() == OrderStatus.CANCEL).count();

                dailySalesStatisticsRepository.save(DailySalesStatistics.create(
                        yesterday, totalOrderCount, totalRevenue,
                        totalDiscountAmount, averageOrderPrice, cancelledOrderCount));

                log.info("[DailySalesBatch] Saved for {}: orders={}, revenue={}, cancelled={}",
                        yesterday, totalOrderCount, totalRevenue, cancelledOrderCount);
            } finally {
                em.close();
            }

            return RepeatStatus.FINISHED;
        };
    }
}
