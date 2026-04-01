package com.example.order;

import com.example.order.domain.Member;
import com.example.order.domain.Order;
import com.example.order.domain.OrderItem;
import com.example.order.domain.Product;
import com.example.order.repository.MemberRepository;
import com.example.order.repository.OrderRepository;
import com.example.order.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitData implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            log.info("[InitData] Data already exists. Skipping seed.");
            return;
        }

        log.info("[InitData] Seeding initial data");

        List<Member> members = seedMembers();
        List<Product> products = seedProducts();
        seedOrders(members, products);

        log.info("[InitData] Seed complete - {} members, {} products, 300 orders",
                members.size(), products.size());
    }

    private List<Member> seedMembers() {
        String[] firstNames = {"James", "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella",
                               "Lucas", "Mia", "Oliver", "Charlotte", "Aiden", "Amelia", "Elijah", "Harper", "Logan", "Evelyn",
                               "Alexander", "Abigail", "Sebastian", "Emily", "Daniel", "Ella", "Henry", "Scarlett", "Jack", "Grace"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Wilson", "Taylor",
                              "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Moore", "Clark", "Lewis",
                              "Walker", "Hall", "Allen", "Young", "King", "Wright", "Scott", "Adams", "Baker", "Nelson"};

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String name = firstNames[i] + " " + lastNames[i];
            String email = firstNames[i].toLowerCase() + "." + lastNames[i].toLowerCase() + "@example.com";
            members.add(memberRepository.save(Member.createMember(name, email)));
        }
        return members;
    }

    private List<Product> seedProducts() {
        String[][] productData = {
                {"MacBook Pro 16", "3500000", "50"},
                {"MacBook Air 15", "1900000", "80"},
                {"iMac 24", "2200000", "30"},
                {"iPhone 16 Pro", "1550000", "200"},
                {"iPhone 16", "1250000", "300"},
                {"iPad Pro", "1730000", "100"},
                {"iPad Air", "930000", "150"},
                {"Apple Watch Ultra", "1150000", "70"},
                {"Apple Watch SE", "350000", "200"},
                {"AirPods Pro", "350000", "500"},
                {"AirPods Max", "770000", "60"},
                {"Galaxy S25 Ultra", "1350000", "250"},
                {"Galaxy Book Pro", "1800000", "80"},
                {"Galaxy Tab S10", "1100000", "120"},
                {"Galaxy Buds Pro", "230000", "300"},
                {"LG Gram 17", "1900000", "60"},
                {"Sony WH-1000XM5", "450000", "150"},
                {"Sony PS5 Pro", "800000", "40"},
                {"Nintendo Switch 2", "550000", "100"},
                {"Dyson Airwrap", "650000", "80"},
                {"Logitech MX Master", "150000", "200"},
                {"Logitech MX Keys", "130000", "200"},
                {"Samsung Monitor 32", "550000", "90"},
                {"LG Monitor 27", "480000", "110"},
                {"Keychron K3 Pro", "140000", "150"},
                {"Leopold FC750R", "180000", "100"},
                {"Elgato Stream Deck", "200000", "70"},
                {"Raspberry Pi 5", "120000", "200"},
                {"Apple Magic Keyboard", "400000", "100"},
                {"Apple Magic Trackpad", "180000", "120"},
        };

        List<Product> products = new ArrayList<>();
        for (String[] data : productData) {
            products.add(productRepository.save(
                    Product.createProduct(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]))));
        }
        return products;
    }

    private void seedOrders(List<Member> members, List<Product> products) {
        Random random = new Random(42);

        for (int i = 0; i < 300; i++) {
            Member member = members.get(random.nextInt(members.size()));

            int itemCount = random.nextInt(3) + 1;
            List<OrderItem> orderItems = new ArrayList<>();

            for (int j = 0; j < itemCount; j++) {
                Product product = products.get(random.nextInt(products.size()));
                int count = random.nextInt(3) + 1;

                if (product.getStockQuantity() < count) {
                    continue;
                }
                orderItems.add(OrderItem.createOrderItem(product, product.getPrice(), count));
            }

            if (!orderItems.isEmpty()) {
                orderRepository.save(Order.createOrder(member, orderItems));
            }
        }
    }
}
