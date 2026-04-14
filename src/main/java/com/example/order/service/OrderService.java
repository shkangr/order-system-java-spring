package com.example.order.service;

import com.example.order.config.annotation.Auditable;
import com.example.order.domain.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
import com.example.order.event.OrderCancelledEvent;
import com.example.order.event.OrderCreatedEvent;
import com.example.order.exception.EntityNotFoundException;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create order with delivery, payment, and optional coupon.
     *
     * Flow:
     *   1. Deduct stock (Pessimistic Lock on Product)
     *   2. Apply coupon if provided (Pessimistic Lock on MemberCoupon)
     *   3. Create order (Cascade saves delivery + payment)
     *   4. Publish Spring Event (OrderCreatedEvent)
     *      → [AFTER_COMMIT] OrderKafkaProducer listens and forwards to Kafka
     */
    @Auditable(action = "CREATE_ORDER")
    @Transactional
    public Long createOrder(CreateOrderRequest request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Member not found. id=" + request.getMemberId()));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getOrderItems()) {
            // Pessimistic Lock: SELECT ... FOR UPDATE to prevent overselling
            Product product = productRepository.findByIdWithLock(itemRequest.getProductId())
                    .orElseThrow(() -> new EntityNotFoundException("Product not found. id=" + itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), itemRequest.getCount());
            orderItems.add(orderItem);
        }

        // Create delivery
        Address address = new Address(request.getZipCode(), request.getAddress1(), request.getAddress2());
        Delivery delivery = Delivery.createDelivery(request.getReceiverName(), request.getPhone(), address);

        // Calculate total price
        int totalPrice = orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();

        // Apply coupon if provided
        int discountAmount = 0;
        Long memberCouponId = request.getMemberCouponId();
        if (memberCouponId != null) {
            discountAmount = couponService.useCoupon(memberCouponId, null, totalPrice);
        }

        // Create payment (with discounted price)
        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
        Payment payment = Payment.createPayment(paymentMethod, totalPrice - discountAmount);

        // Create order (cascade saves delivery + payment)
        Order order = Order.createOrder(member, orderItems, delivery, payment, memberCouponId, discountAmount);
        orderRepository.save(order);

        // Publish Spring Event → OrderKafkaProducer listens (AFTER_COMMIT) and forwards to Kafka
        eventPublisher.publishEvent(
                new OrderCreatedEvent(order.getId(), member.getName(), totalPrice, discountAmount));

        return order.getId();
    }

    /**
     * Cancel order.
     *
     * Flow:
     *   1. Cancel order (restores stock, cancels delivery and payment)
     *   2. Publish OrderCancelledEvent
     *      → [BEFORE_COMMIT] CouponEventListener restores coupon (same transaction)
     *      → [AFTER_COMMIT]  Kafka producer sends notification
     */
    @Auditable(action = "CANCEL_ORDER")
    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findWithAllById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found. id=" + orderId));

        order.cancel();

        // Publish Spring Event
        //   → CouponEventListener (BEFORE_COMMIT): restores coupon in same transaction
        //   → OrderKafkaProducer (AFTER_COMMIT): listens and forwards to Kafka
        eventPublisher.publishEvent(
                new OrderCancelledEvent(order.getId(), order.getMember().getName(), order.getMemberCouponId()));
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
