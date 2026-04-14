package com.example.order.repository;

import com.example.order.domain.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long>, CouponRepositoryCustom {

    /**
     * Pessimistic Lock for coupon issuance.
     * SELECT ... FOR UPDATE — prevents concurrent over-issuance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where c.id = :id")
    Optional<Coupon> findByIdWithLock(@Param("id") Long id);
}
