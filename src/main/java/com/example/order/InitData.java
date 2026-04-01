package com.example.order;

import com.example.order.domain.Member;
import com.example.order.domain.Product;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Member member1 = Member.createMember("김철수", "kim@example.com");
        Member member2 = Member.createMember("이영희", "lee@example.com");
        memberRepository.save(member1);
        memberRepository.save(member2);

        Product product1 = Product.createProduct("맥북 프로", 3_000_000, 100);
        Product product2 = Product.createProduct("아이폰 16", 1_500_000, 200);
        Product product3 = Product.createProduct("에어팟 프로", 350_000, 500);
        productRepository.save(product1);
        productRepository.save(product2);
        productRepository.save(product3);
    }
}
