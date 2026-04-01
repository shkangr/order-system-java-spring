package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.EntityNotFoundException;
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
    private final SlackNotificationService slackNotificationService;

    /**
     * Create order
     * - Stock deduction handled inside OrderItem.createOrderItem()
     * - cascade = ALL, so OrderItems are saved with Order
     */
    @Transactional
    public Long createOrder(CreateOrderRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Member not found. id=" + request.getMemberId()));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found. id=" + itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), itemRequest.getCount());
            orderItems.add(orderItem);
        }

        Order order = Order.createOrder(member, orderItems);
        orderRepository.save(order);

        slackNotificationService.sendOrderCreatedMessage(
                order.getId(), member.getName(), order.getTotalPrice());

        return order.getId();
    }

    /**
     * Cancel order
     * - Stock restoration handled inside OrderItem.cancel()
     */
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findWithAllById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found. id=" + orderId));

        order.cancel();

        slackNotificationService.sendOrderCancelledMessage(
                order.getId(), order.getMember().getName());
    }

    /**
     * Find single order - fetch join to solve N+1
     */
    public OrderResponse findOrder(Long orderId) {
        Order order = orderRepository.findWithAllById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found. id=" + orderId));

        return new OrderResponse(order);
    }

    /**
     * Find all orders - fetch join to solve N+1
     */
    public List<OrderResponse> findAllOrders() {
        return orderRepository.findAllWithMemberAndOrderItems().stream()
                .map(OrderResponse::new)
                .toList();
    }

    /**
     * Find orders by member
     */
    public List<OrderResponse> findOrdersByMemberId(Long memberId) {
        return orderRepository.findAllByMemberIdWithMember(memberId).stream()
                .map(OrderResponse::new)
                .toList();
    }
}
