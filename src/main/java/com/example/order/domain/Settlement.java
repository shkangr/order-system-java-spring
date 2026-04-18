package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Monthly settlement per member — populated by MonthlySettlementJob.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"memberId", "settlementMonth"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    private String memberName;

    @Column(nullable = false)
    private LocalDate settlementMonth;   // first day of the month (e.g., 2026-03-01)

    private long totalOrderCount;
    private long totalAmount;
    private long totalDiscountAmount;
    private long settlementAmount;       // totalAmount - totalDiscountAmount

    public static Settlement create(Long memberId, String memberName, LocalDate settlementMonth,
                                    long totalOrderCount, long totalAmount,
                                    long totalDiscountAmount) {
        Settlement s = new Settlement();
        s.memberId = memberId;
        s.memberName = memberName;
        s.settlementMonth = settlementMonth;
        s.totalOrderCount = totalOrderCount;
        s.totalAmount = totalAmount;
        s.totalDiscountAmount = totalDiscountAmount;
        s.settlementAmount = totalAmount - totalDiscountAmount;
        return s;
    }
}
