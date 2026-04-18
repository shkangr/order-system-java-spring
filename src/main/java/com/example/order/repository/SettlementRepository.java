package com.example.order.repository;

import com.example.order.domain.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findAllBySettlementMonth(LocalDate settlementMonth);

    boolean existsByMemberIdAndSettlementMonth(Long memberId, LocalDate settlementMonth);
}
