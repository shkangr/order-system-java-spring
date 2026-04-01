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
    @DisplayName("주문 생성 - 재고 차감 확인")
    void createOrder() {
        // given
        Member member = Member.createMember("테스트회원", "test@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("테스트상품", 10000, 50);
        productRepository.save(product);

        em.flush();
        em.clear();

        // CreateOrderRequest를 직접 생성하기 어려우므로 도메인 로직으로 테스트
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
        assertThat(savedOrder.getMember().getName()).isEqualTo("테스트회원");

        // 재고 차감 확인
        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(47); // 50 - 3
    }

    @Test
    @DisplayName("주문 취소 - 재고 복구 확인")
    void cancelOrder() {
        // given
        Member member = Member.createMember("테스트회원", "test@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("테스트상품", 10000, 50);
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
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(50); // 복구됨
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void notEnoughStock() {
        // given
        Product product = Product.createProduct("테스트상품", 10000, 5);
        productRepository.save(product);

        // when & then
        assertThatThrownBy(() -> OrderItem.createOrderItem(product, product.getPrice(), 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고가 부족합니다");
    }

    @Test
    @DisplayName("이미 취소된 주문 재취소 시 예외 발생")
    void cancelAlreadyCancelledOrder() {
        // given
        Member member = Member.createMember("테스트회원", "test@test.com");
        memberRepository.save(member);

        Product product = Product.createProduct("테스트상품", 10000, 50);
        productRepository.save(product);

        OrderItem orderItem = OrderItem.createOrderItem(product, product.getPrice(), 3);
        Order order = Order.createOrder(member, java.util.List.of(orderItem));
        orderRepository.save(order);

        order.cancel(); // 첫 번째 취소

        // when & then
        assertThatThrownBy(order::cancel)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 취소된 주문");
    }

    @Test
    @DisplayName("주문 총액 계산")
    void calculateTotalPrice() {
        // given
        Member member = Member.createMember("테스트회원", "test@test.com");
        memberRepository.save(member);

        Product product1 = Product.createProduct("상품A", 10000, 100);
        Product product2 = Product.createProduct("상품B", 20000, 100);
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
    @DisplayName("Fetch Join - N+1 문제 해결 확인")
    void fetchJoinTest() {
        // given
        Member member = Member.createMember("테스트회원", "test@test.com");
        memberRepository.save(member);

        Product product1 = Product.createProduct("상품A", 10000, 100);
        Product product2 = Product.createProduct("상품B", 20000, 100);
        productRepository.save(product1);
        productRepository.save(product2);

        OrderItem item1 = OrderItem.createOrderItem(product1, product1.getPrice(), 1);
        OrderItem item2 = OrderItem.createOrderItem(product2, product2.getPrice(), 1);
        Order order = Order.createOrder(member, java.util.List.of(item1, item2));
        orderRepository.save(order);

        em.flush();
        em.clear();

        // when - fetch join으로 한 번의 쿼리로 모든 데이터 조회
        System.out.println("===== Fetch Join 쿼리 시작 =====");
        Order fetchedOrder = orderRepository.findWithAllById(order.getId()).orElseThrow();

        // then - 추가 쿼리 없이 접근 가능
        System.out.println("===== 데이터 접근 (추가 쿼리 없음) =====");
        assertThat(fetchedOrder.getMember().getName()).isEqualTo("테스트회원");
        assertThat(fetchedOrder.getOrderItems()).hasSize(2);
        assertThat(fetchedOrder.getOrderItems().get(0).getProduct().getName()).isNotNull();
        System.out.println("===== 테스트 완료 =====");
    }
}
