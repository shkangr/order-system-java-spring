package com.example.order.repository;

import com.example.order.domain.DailySalesStatistics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailySalesStatisticsRepository extends JpaRepository<DailySalesStatistics, Long> {

    Optional<DailySalesStatistics> findBySalesDate(LocalDate salesDate);

    boolean existsBySalesDate(LocalDate salesDate);
}
