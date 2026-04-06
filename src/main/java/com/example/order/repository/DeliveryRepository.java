package com.example.order.repository;

import com.example.order.domain.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    /**
     * SELECT ... FOR UPDATE
     * Prevents concurrent status transitions on the same delivery.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id")
    Optional<Delivery> findByIdWithLock(@Param("id") Long id);
}
