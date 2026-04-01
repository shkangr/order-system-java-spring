package com.example.order.service;

import com.example.order.config.annotation.Auditable;
import com.example.order.domain.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Create order with delivery and payment
     */
    @Auditable(action = "CREATE_ORDER")
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

        // Create delivery
        Address address = new Address(request.getZipCode(), request.getAddress1(), request.getAddress2());
        Delivery delivery = Delivery.createDelivery(request.getReceiverName(), request.getPhone(), address);

        // Create payment
        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        int totalPrice = orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();
        Payment payment = Payment.createPayment(paymentMethod, totalPrice);

        // Create order (cascade saves delivery + payment)
        Order order = Order.createOrder(member, orderItems, delivery, payment);
        orderRepository.save(order);

        slackNotificationService.sendOrderCreatedMessage(
                order.getId(), member.getName(), order.getTotalPrice());

        return order.getId();
    }

    /**
     * Cancel order
     * - Validates delivery status (cannot cancel if shipping/completed)
     * - Restores stock, cancels delivery and payment
     */
    @Auditable(action = "CANCEL_ORDER")
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
     * Find orders with pagination
     */
    public Page<OrderResponse> findOrdersPaged(Pageable pageable) {
        return orderRepository.findAllWithMember(pageable)
                .map(OrderResponse::new);
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
