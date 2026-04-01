package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired OrderService orderService;
    @Autowired MemberRepository memberRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired EntityManager em;

    private Order createTestOrder(Member member, Product product, int count) {
        OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), count);
        Address address = new Address("10001", "123 Main St", "New York");
        Delivery delivery = Delivery.createDelivery(member.getName(), "010-1234-5678", address);
        Payment payment = Payment.createPayment(PaymentMethod.CARD, orderItem.getTotalPrice());
        return Order.createOrder(member, java.util.List.of(orderItem), delivery, payment);
    }

    @Test
    @DisplayName("Create order - verify stock deduction")
    void createOrder() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("Test Product", 10000, 50);
        productRepository.save(product);

        em.flush();
        em.clear();

        Product foundProduct = productRepository.findById(product.getId()).orElseThrow();
        Member foundMember = memberRepository.findById(member.getId()).orElseThrow();

        Order order = createTestOrder(foundMember, foundProduct, 3);
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        Order savedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.ORDER);
        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getTotalPrice()).isEqualTo(10000 * 3);
        assertThat(savedOrder.getMember().getName()).isEqualTo("John Doe");
        assertThat(savedOrder.getDelivery().getStatus()).isEqualTo(DeliveryStatus.READY);
        assertThat(savedOrder.getPayment().getStatus()).isEqualTo(PaymentStatus.READY);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(47); // 50 - 3
    }

    @Test
    @DisplayName("Cancel order - verify stock restoration and delivery/payment cancel")
    void cancelOrder() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("Test Product", 10000, 50);
        productRepository.save(product);

        Order order = createTestOrder(member, product, 5);
        order.getPayment().approve(); // pay first so cancel can work
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        Order savedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();
        savedOrder.cancel();

        em.flush();
        em.clear();

        // then
        Order cancelledOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCEL);
        assertThat(cancelledOrder.getDelivery().getStatus()).isEqualTo(DeliveryStatus.CANCELED);
        assertThat(cancelledOrder.getPayment().getStatus()).isEqualTo(PaymentStatus.CANCELED);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(50); // restored
    }

    @Test
    @DisplayName("Cannot cancel order when delivery is shipping")
    void cannotCancelWhenShipping() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("Test Product", 10000, 50);
        productRepository.save(product);

        Order order = createTestOrder(member, product, 3);
        orderRepository.save(order);

        order.getDelivery().startShipping();

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHIPPING");
    }

    @Test
    @DisplayName("Throw exception when stock is insufficient")
    void notEnoughStock() {
        // given
        Product product = Product.createProduct("Test Product", 10000, 5);
        productRepository.save(product);

        // when & then
        assertThatThrownBy(() -> OrderItem.createOrderItem(product, product.getPrice(), 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not enough stock");
    }

    @Test
    @DisplayName("Throw exception when cancelling already cancelled order")
    void cancelAlreadyCancelledOrder() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("Test Product", 10000, 50);
        productRepository.save(product);

        Order order = createTestOrder(member, product, 3);
        orderRepository.save(order);

        order.cancel();

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    @DisplayName("Calculate total price")
    void calculateTotalPrice() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product1 = Product.createProduct("Product A", 10000, 100);
        Product product2 = Product.createProduct("Product B", 20000, 100);
        productRepository.save(product1);
        productRepository.save(product2);

        OrderItem item1 = OrderItem.createOrderItem(product1, product1.getPrice(), 2); // 20,000
        OrderItem item2 = OrderItem.createOrderItem(product2, product2.getPrice(), 3); // 60,000
        Address address = new Address("10001", "123 Main St", "New York");
        Delivery delivery = Delivery.createDelivery("John Doe", "010-1234-5678", address);
        Payment payment = Payment.createPayment(PaymentMethod.CARD, 80000);
        Order order = Order.createOrder(member, java.util.List.of(item1, item2), delivery, payment);
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        Order savedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then
        assertThat(savedOrder.getTotalPrice()).isEqualTo(80000);
    }

    @Test
    @DisplayName("Fetch Join - verify N+1 problem is resolved")
    void fetchJoinTest() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product1 = Product.createProduct("Product A", 10000, 100);
        Product product2 = Product.createProduct("Product B", 20000, 100);
        productRepository.save(product1);
        productRepository.save(product2);

        OrderItem item1 = OrderItem.createOrderItem(product1, product1.getPrice(), 1);
        OrderItem item2 = OrderItem.createOrderItem(product2, product2.getPrice(), 1);
        Address address = new Address("10001", "123 Main St", "New York");
        Delivery delivery = Delivery.createDelivery("John Doe", "010-1234-5678", address);
        Payment payment = Payment.createPayment(PaymentMethod.CARD, 30000);
        Order order = Order.createOrder(member, java.util.List.of(item1, item2), delivery, payment);
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        System.out.println("===== Fetch Join Query Start =====");
        Order fetchedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then
        System.out.println("===== Data Access (no extra queries) =====");
        assertThat(fetchedOrder.getMember().getName()).isEqualTo("John Doe");
        assertThat(fetchedOrder.getOrderItems()).hasSize(2);
        assertThat(fetchedOrder.getOrderItems().get(0).getProduct().getName()).isNotNull();
        assertThat(fetchedOrder.getDelivery().getReceiverName()).isEqualTo("John Doe");
        assertThat(fetchedOrder.getPayment().getAmount()).isEqualTo(30000);
        System.out.println("===== Test Complete =====");
    }
}
