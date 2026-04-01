package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    /**
     * 주문 생성
     * - 재고 차감은 OrderItem.createOrderItem() 내부에서 처리
     * - cascade = ALL 이므로 Order 저장 시 OrderItem도 함께 저장
     */
    @Transactional
    public Long createOrder(CreateOrderRequest request) {
        // 회원 조회
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다. id=" + request.getMemberId()));

        // 주문 아이템 생성
        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다. id=" + itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), itemRequest.getCount());
            orderItems.add(orderItem);
        }

        // 주문 생성 (연관관계 편의 메서드 동작)
        Order order = Order.createOrder(member, orderItems);

        // 저장 (cascade로 OrderItem도 함께 저장)
        orderRepository.save(order);

        return order.getId();
    }

    /**
     * 주문 취소
     * - 재고 복구는 OrderItem.cancel() 내부에서 처리
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findWithAllById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. id=" + orderId));

        order.cancel();
    }

    /**
     * 단건 주문 조회 - fetch join으로 N+1 해결
     */
    public OrderResponse findOrder(Long orderId) {
        Order order = orderRepository.findWithAllById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다. id=" + orderId));

        return new OrderResponse(order);
    }

    /**
     * 전체 주문 조회 - fetch join으로 N+1 해결
     */
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAllWithMemberAndOrderItems().stream()
                .map(OrderResponse::new)
                .toList();
    }

    /**
     * 특정 회원 주문 조회
     */
    public List<OrderResponse> findOrdersByMemberId(Long memberId) {
        return orderRepository.findAllByMemberIdWithMember(memberId).stream()
                .map(OrderResponse::new)
                .toList();
    }
}
