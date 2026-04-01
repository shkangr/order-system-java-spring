package com.example.order.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SlackNotificationService {

    @Async("notificationExecutor")
    public void sendOrderCreatedMessage(Long orderId, String memberName, int totalPrice) {
        log.info("[Slack] 주문 알림 전송 시작 - thread={}", Thread.currentThread().getName());

        // 실제 Slack API 호출 시뮬레이션 (네트워크 지연)
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String message = String.format(
                ":shopping_cart: 새 주문이 발생했습니다!\n" +
                "• 주문번호: %d\n" +
                "• 주문자: %s\n" +
                "• 총액: %,d원",
                orderId, memberName, totalPrice
        );

        log.info("[Slack] 메시지 전송 완료 - {}", message);
    }

    @Async("notificationExecutor")
    public void sendOrderCancelledMessage(Long orderId, String memberName) {
        log.info("[Slack] 취소 알림 전송 시작 - thread={}", Thread.currentThread().getName());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String message = String.format(
                ":x: 주문이 취소되었습니다!\n" +
                "• 주문번호: %d\n" +
                "• 주문자: %s",
                orderId, memberName
        );

        log.info("[Slack] 메시지 전송 완료 - {}", message);
    }
}
