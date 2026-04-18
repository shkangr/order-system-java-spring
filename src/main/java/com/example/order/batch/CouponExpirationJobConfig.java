package com.example.order.batch;

import com.example.order.domain.MemberCoupon;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.database.JpaPagingItemReader;
import org.springframework.batch.infrastructure.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Batch Job: expires ISSUED coupons past their validTo date.
 *
 * Chunk-based: Reader → Processor → Writer (100 items per chunk)
 * Scheduled: every day at midnight
 */
@Configuration
public class CouponExpirationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    public CouponExpirationJobConfig(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     EntityManagerFactory entityManagerFactory) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Bean
    public Job couponExpirationJob() {
        return new JobBuilder("couponExpirationJob", jobRepository)
                .start(couponExpirationStep())
                .build();
    }

    @Bean
    public Step couponExpirationStep() {
        return new StepBuilder("couponExpirationStep", jobRepository)
                .<MemberCoupon, MemberCoupon>chunk(100, transactionManager)
                .reader(expiredCouponReader())
                .processor(expiredCouponProcessor())
                .writer(expiredCouponWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<MemberCoupon> expiredCouponReader() {
        return new JpaPagingItemReaderBuilder<MemberCoupon>()
                .name("expiredCouponReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(
                        "SELECT mc FROM MemberCoupon mc JOIN FETCH mc.coupon c " +
                        "WHERE mc.status = 'ISSUED' AND c.validTo < :now"
                )
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .pageSize(100)
                .build();
    }

    @Bean
    public ItemProcessor<MemberCoupon, MemberCoupon> expiredCouponProcessor() {
        return memberCoupon -> {
            memberCoupon.expire();
            return memberCoupon;
        };
    }

    @Bean
    public JpaItemWriter<MemberCoupon> expiredCouponWriter() {
        return new JpaItemWriterBuilder<MemberCoupon>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
