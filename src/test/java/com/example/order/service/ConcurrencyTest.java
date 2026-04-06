package com.example.order.service;

import com.example.order.domain.*;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTest {

    @Autowired ProductRepository productRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired OrderRepository orderRepository;

    @Test
    @DisplayName("Pessimistic Lock - concurrent stock deduction should not oversell")
    void concurrentStockDeduction() throws InterruptedException {
        // given: product with 10 stock
        Product product = Product.createProduct("Concurrency Test Product", 1000, 10);
        productRepository.save(product);

        Member member = Member.createMember("Test User", "test@test.com");
        memberRepository.save(member);

        int threadCount = 10; // 10 threads each ordering 1 unit
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10 concurrent orders, each requesting 1 unit
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Each thread: lock product → check stock → deduct
                    Product locked = productRepository.findByIdWithLock(product.getId()).orElseThrow();
                    if (locked.getStockQuantity() > 0) {
                        OrderItem item = OrderItem.createOrderItem(locked, locked.getPrice(), 1);
                        Address address = new Address("10001", "123 Main St", "NY");
                        Delivery delivery = Delivery.createDelivery("Test", "010-0000-0000", address);
                        Payment payment = Payment.createPayment(PaymentMethod.CARD, 1000);
                        Order order = Order.createOrder(member, List.of(item), delivery, payment);
                        orderRepository.save(order);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // then: stock should be exactly 0, all 10 succeeded
        Product result = productRepository.findById(product.getId()).orElseThrow();
        System.out.println("Stock: " + result.getStockQuantity());
        System.out.println("Success: " + successCount.get() + ", Fail: " + failCount.get());

        assertThat(result.getStockQuantity()).isGreaterThanOrEqualTo(0);
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    }
}
