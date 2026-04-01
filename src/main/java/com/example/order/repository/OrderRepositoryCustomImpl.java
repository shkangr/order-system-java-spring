package com.example.order.repository;

import com.example.order.domain.Order;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.order.domain.QMember.member;
import static com.example.order.domain.QOrder.order;
import static com.example.order.domain.QOrderItem.orderItem;
import static com.example.order.domain.QProduct.product;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Order> findWithMemberById(Long orderId) {
        Order result = queryFactory
                .selectFrom(order)
                .join(order.member, member).fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<Order> findWithAllById(Long orderId) {
        Order result = queryFactory
                .selectFrom(order)
                .join(order.member, member).fetchJoin()
                .join(order.orderItems, orderItem).fetchJoin()
                .join(orderItem.product, product).fetchJoin()
                .where(order.id.eq(orderId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Order> findAllWithMemberAndOrderItems() {
        return queryFactory
                .selectFrom(order).distinct()
                .join(order.member, member).fetchJoin()
                .join(order.orderItems, orderItem).fetchJoin()
                .join(orderItem.product, product).fetchJoin()
                .fetch();
    }

    @Override
    public List<Order> findAllByMemberIdWithMember(Long memberId) {
        return queryFactory
                .selectFrom(order)
                .join(order.member, member).fetchJoin()
                .where(order.member.id.eq(memberId))
                .fetch();
    }
}
