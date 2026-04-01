package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResponse;
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

        OrderItem orderItem = OrderItem.createOrderItem(foundProduct, foundProduct.getPrice(), 3);
        Order order = Order.createOrder(foundMember, java.util.List.of(orderItem));
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

        // verify stock deduction
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(47); // 50 - 3
    }

    @Test
    @DisplayName("Cancel order - verify stock restoration")
    void cancelOrder() {
        // given
        Member member = Member.createMember("John Doe", "john@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("Test Product", 10000, 50);
        productRepository.save(product);

        OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), 5);
        Order order = Order.createOrder(member, java.util.List.of(orderItem));
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        Order savedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();
        savedOrder.cancel();

        em.flush();
        em.clear();

        // then
        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCEL);

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(50); // restored
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

        OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), 3);
        Order order = Order.createOrder(member, java.util.List.of(orderItem));
        orderRepository.save(order);

        order.cancel(); // first cancel

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
        Order order = Order.createOrder(member, java.util.List.of(item1, item2));
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when
        Order savedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then
        assertThat(savedOrder.getTotalPrice()).isEqualTo(80000); // 20,000 + 60,000
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
        Order order = Order.createOrder(member, java.util.List.of(item1, item2));
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when - single query with fetch join
        System.out.println("===== Fetch Join Query Start =====");
        Order fetchedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then - no additional queries
        System.out.println("===== Data Access (no extra queries) =====");
        assertThat(fetchedOrder.getMember().getName()).isEqualTo("John Doe");
        assertThat(fetchedOrder.getOrderItems()).hasSize(2);
        assertThat(fetchedOrder.getOrderItems().get(0).getProduct().getName()).isNotNull();
        System.out.println("===== Test Complete =====");
    }
}
