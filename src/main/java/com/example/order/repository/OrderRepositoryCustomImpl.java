package com.example.order.repository;

import com.example.order.domain.Order;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.order.domain.QDelivery.delivery;
import static com.example.order.domain.QMember.member;
import static com.example.order.domain.QOrder.order;
import static com.example.order.domain.QOrderItem.orderItem;
import static com.example.order.domain.QPayment.payment;
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
                .leftJoin(order.delivery, delivery).fetchJoin()
                .leftJoin(order.payment, payment).fetchJoin()
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
                .leftJoin(order.delivery, delivery).fetchJoin()
                .leftJoin(order.payment, payment).fetchJoin()
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

    @Override
    public Page<Order> findAllWithMember(Pageable pageable) {
        List<Order> content = queryFactory
                .selectFrom(order)
                .join(order.member, member).fetchJoin()
                .leftJoin(order.delivery, delivery).fetchJoin()
                .leftJoin(order.payment, payment).fetchJoin()
                .orderBy(order.id.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        var countQuery = queryFactory
                .select(order.count())
                .from(order);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
