package com.example.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted = false")
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private int discountValue;

    private int totalQuantity;

    private int issuedCount;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private int minOrderAmount;

    @Version
    private Long version;

    private boolean deleted = false;

    @OneToMany(mappedBy = "coupon")
    private List<MemberCoupon> memberCoupons = new ArrayList<>();

    // === Factory Method === //

    public static Coupon createCoupon(String name, DiscountType discountType, int discountValue,
                                      int totalQuantity, LocalDateTime validFrom, LocalDateTime validTo,
                                      int minOrderAmount) {
        Coupon coupon = new Coupon();
        coupon.name = name;
        coupon.discountType = discountType;
        coupon.discountValue = discountValue;
        coupon.totalQuantity = totalQuantity;
        coupon.issuedCount = 0;
        coupon.validFrom = validFrom;
        coupon.validTo = validTo;
        coupon.minOrderAmount = minOrderAmount;
        return coupon;
    }

    // === Business Logic === //

    /**
     * Issue one coupon (increment issued count).
     * Called under Pessimistic Lock to prevent over-issuance.
     */
    public void issue() {
        if (this.issuedCount >= this.totalQuantity) {
            throw new IllegalStateException("Coupon is sold out. total=" + totalQuantity + ", issued=" + issuedCount);
        }
        if (LocalDateTime.now().isAfter(this.validTo)) {
            throw new IllegalStateException("Coupon has expired. validTo=" + validTo);
        }
        this.issuedCount++;
    }

    /**
     * Restore one issued count (when coupon usage is cancelled).
     */
    public void restoreQuantity() {
        if (this.issuedCount <= 0) {
            throw new IllegalStateException("Cannot restore. issuedCount is already 0.");
        }
        this.issuedCount--;
    }

    /**
     * Calculate discount amount for the given order price.
     */
    public int calculateDiscount(int orderPrice) {
        if (orderPrice < this.minOrderAmount) {
            throw new IllegalStateException(
                    "Order price must be at least " + minOrderAmount + " to use this coupon.");
        }
        return switch (this.discountType) {
            case RATE -> orderPrice * discountValue / 100;
            case FIXED -> Math.min(discountValue, orderPrice);
        };
    }

    /**
     * Check if the coupon is currently valid (within date range).
     */
    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return !this.deleted && now.isAfter(validFrom) && now.isBefore(validTo);
    }

    /**
     * Soft delete
     */
    public void softDelete() {
        this.deleted = true;
    }

    /**
     * Update coupon info (protected by Optimistic Lock via @Version)
     */
    public void updateInfo(String name, int discountValue, int minOrderAmount) {
        this.name = name;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
    }
}
