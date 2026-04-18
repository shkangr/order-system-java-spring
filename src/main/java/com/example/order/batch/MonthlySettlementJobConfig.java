package com.example.order.batch;

import com.example.order.domain.Order;
import com.example.order.domain.OrderStatus;
import com.example.order.domain.Settlement;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Batch Job: calculates monthly settlement per member.
 *
 * Chunk-based: Reader (member IDs) → Processor (aggregate orders) → Writer (save Settlement)
 * Scheduled: 1st of each month at 3am
 * JobParameter "targetMonth" (yyyy-MM): override target month, defaults to previous month
 */
@Configuration
public class MonthlySettlementJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @PersistenceContext
    private EntityManager entityManager;

    public MonthlySettlementJobConfig(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      EntityManagerFactory entityManagerFactory) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Bean
    public Job monthlySettlementJob() {
        return new JobBuilder("monthlySettlementJob", jobRepository)
                .start(monthlySettlementStep())
                .build();
    }

    @Bean
    public Step monthlySettlementStep() {
        return new StepBuilder("monthlySettlementStep", jobRepository)
                .<Long, Settlement>chunk(50, transactionManager)
                .reader(settlementMemberIdReader(null))
                .processor(settlementProcessor(null))
                .writer(settlementWriter())
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Long> settlementMemberIdReader(
            @Value("#{jobParameters['targetMonth']}") String targetMonth) {

        YearMonth yearMonth = resolveTargetMonth(targetMonth);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);

        List<Long> memberIds = entityManager.createQuery(
                        "SELECT DISTINCT o.member.id FROM Order o " +
                        "WHERE o.orderDate >= :start AND o.orderDate <= :end " +
                        "AND o.status <> :cancelStatus", Long.class)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("cancelStatus", OrderStatus.CANCEL)
                .getResultList();

        return new ListItemReader<>(memberIds);
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, Settlement> settlementProcessor(
            @Value("#{jobParameters['targetMonth']}") String targetMonth) {

        YearMonth yearMonth = resolveTargetMonth(targetMonth);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999);
        LocalDate settlementMonth = yearMonth.atDay(1);

        return memberId -> {
            List<Order> orders = entityManager.createQuery(
                            "SELECT o FROM Order o JOIN FETCH o.orderItems " +
                            "WHERE o.member.id = :memberId " +
                            "AND o.orderDate >= :start AND o.orderDate <= :end " +
                            "AND o.status <> :cancelStatus", Order.class)
                    .setParameter("memberId", memberId)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .setParameter("cancelStatus", OrderStatus.CANCEL)
                    .getResultList();

            if (orders.isEmpty()) return null;

            long totalAmount = orders.stream().mapToLong(Order::getTotalPrice).sum();
            long totalDiscount = orders.stream().mapToLong(Order::getDiscountAmount).sum();
            String memberName = orders.get(0).getMember().getName();

            return Settlement.create(memberId, memberName, settlementMonth,
                    orders.size(), totalAmount, totalDiscount);
        };
    }

    @Bean
    public JpaItemWriter<Settlement> settlementWriter() {
        return new JpaItemWriterBuilder<Settlement>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    private YearMonth resolveTargetMonth(String targetMonth) {
        if (targetMonth != null && !targetMonth.isBlank()) {
            return YearMonth.parse(targetMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return YearMonth.now().minusMonths(1);
    }
}
