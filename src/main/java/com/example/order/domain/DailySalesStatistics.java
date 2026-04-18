package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Daily aggregated sales statistics — populated by DailySalesStatisticsJob.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "salesDate"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySalesStatistics extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate salesDate;

    private long totalOrderCount;
    private long totalRevenue;
    private long totalDiscountAmount;
    private long averageOrderPrice;
    private long cancelledOrderCount;

    public static DailySalesStatistics create(LocalDate salesDate, long totalOrderCount,
                                               long totalRevenue, long totalDiscountAmount,
                                               long averageOrderPrice, long cancelledOrderCount) {
        DailySalesStatistics stats = new DailySalesStatistics();
        stats.salesDate = salesDate;
        stats.totalOrderCount = totalOrderCount;
        stats.totalRevenue = totalRevenue;
        stats.totalDiscountAmount = totalDiscountAmount;
        stats.averageOrderPrice = averageOrderPrice;
        stats.cancelledOrderCount = cancelledOrderCount;
        return stats;
    }
}
