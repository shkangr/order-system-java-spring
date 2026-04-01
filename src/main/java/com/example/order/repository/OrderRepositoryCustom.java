package com.example.order.repository;

import com.example.order.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryCustom {

    Optional<Order> findWithMemberById(Long orderId);

    Optional<Order> findWithAllById(Long orderId);

    List<Order> findAllWithMemberAndOrderItems();

    List<Order> findAllByMemberIdWithMember(Long memberId);
}
