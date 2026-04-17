package com.example.order;

import com.example.order.domain.*;
import com.example.order.repository.*;
import com.example.order.service.CouponRedisService;
import com.example.order.service.StockRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final CategoryRepository categoryRepository;
    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final StockRedisService stockRedisService;
    private final CouponRedisService couponRedisService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.count() > 0) {
            log.info("[InitData] Data already exists. Skipping seed.");
            return;
        }

        log.info("[InitData] Seeding initial data");

        List<Category> categories = seedCategories();
        List<Member> members = seedMembers();
        List<Product> products = seedProducts(categories);
        seedOrders(members, products);
        seedCoupons(members);

        // Sync stock and coupon counts to Redis
        stockRedisService.syncAllFromDb();
        couponRedisService.syncAllFromDb();

        log.info("[InitData] Seed complete - {} categories, {} members, {} products, 300 orders, coupons seeded",
                categories.size(), members.size(), products.size());
    }

    private List<Category> seedCategories() {
        // Root categories
        Category laptop = Category.createCategory("Laptop");
        Category phone = Category.createCategory("Phone");
        Category tablet = Category.createCategory("Tablet");
        Category wearable = Category.createCategory("Wearable");
        Category audio = Category.createCategory("Audio");
        Category gaming = Category.createCategory("Gaming");
        Category monitor = Category.createCategory("Monitor");
        Category accessory = Category.createCategory("Accessory");
        categoryRepository.saveAll(List.of(laptop, phone, tablet, wearable, audio, gaming, monitor, accessory));

        // Sub categories
        Category appleLaptop = Category.createCategory("Apple Laptop");
        appleLaptop.setParent(laptop);
        Category otherLaptop = Category.createCategory("Other Laptop");
        otherLaptop.setParent(laptop);

        Category applePhone = Category.createCategory("Apple Phone");
        applePhone.setParent(phone);
        Category androidPhone = Category.createCategory("Android Phone");
        androidPhone.setParent(phone);

        Category keyboard = Category.createCategory("Keyboard");
        keyboard.setParent(accessory);
        Category mouse = Category.createCategory("Mouse & Trackpad");
        mouse.setParent(accessory);

        categoryRepository.saveAll(List.of(appleLaptop, otherLaptop, applePhone, androidPhone, keyboard, mouse));

        return List.of(appleLaptop, otherLaptop, applePhone, androidPhone,
                tablet, wearable, audio, gaming, monitor, keyboard, mouse);
    }

    private List<Member> seedMembers() {
        String[] firstNames = {"James", "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella",
                               "Lucas", "Mia", "Oliver", "Charlotte", "Aiden", "Amelia", "Elijah", "Harper", "Logan", "Evelyn",
                               "Alexander", "Abigail", "Sebastian", "Emily", "Daniel", "Ella", "Henry", "Scarlett", "Jack", "Grace"};
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Wilson", "Taylor",
                              "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Moore", "Clark", "Lewis",
                              "Walker", "Hall", "Allen", "Young", "King", "Wright", "Scott", "Adams", "Baker", "Nelson"};

        String encodedPassword = passwordEncoder.encode("password123");

        List<Member> members = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            String name = firstNames[i] + " " + lastNames[i];
            String email = firstNames[i].toLowerCase() + "." + lastNames[i].toLowerCase() + "@example.com";
            // First 2 members are ADMIN, rest are USER
            Role role = (i < 2) ? Role.ADMIN : Role.USER;
            members.add(memberRepository.save(
                    Member.createMember(name, email, encodedPassword, role)));
        }

        log.info("[InitData] Admin accounts: james.smith@example.com, emma.johnson@example.com (password: password123)");
        return members;
    }

    private List<Product> seedProducts(List<Category> categories) {
        // categories: [appleLaptop, otherLaptop, applePhone, androidPhone,
        //              tablet, wearable, audio, gaming, monitor, keyboard, mouse]
        //  index:       0            1           2            3
        //               4       5          6       7        8       9        10

        Object[][] productData = {
                {"MacBook Pro 16", 3500000, 50, 0},      // Apple Laptop
                {"MacBook Air 15", 1900000, 80, 0},
                {"iMac 24", 2200000, 30, 0},
                {"iPhone 16 Pro", 1550000, 200, 2},      // Apple Phone
                {"iPhone 16", 1250000, 300, 2},
                {"iPad Pro", 1730000, 100, 4},            // Tablet
                {"iPad Air", 930000, 150, 4},
                {"Apple Watch Ultra", 1150000, 70, 5},    // Wearable
                {"Apple Watch SE", 350000, 200, 5},
                {"AirPods Pro", 350000, 500, 6},          // Audio
                {"AirPods Max", 770000, 60, 6},
                {"Galaxy S25 Ultra", 1350000, 250, 3},    // Android Phone
                {"Galaxy Book Pro", 1800000, 80, 1},      // Other Laptop
                {"Galaxy Tab S10", 1100000, 120, 4},      // Tablet
                {"Galaxy Buds Pro", 230000, 300, 6},      // Audio
                {"LG Gram 17", 1900000, 60, 1},           // Other Laptop
                {"Sony WH-1000XM5", 450000, 150, 6},     // Audio
                {"Sony PS5 Pro", 800000, 40, 7},          // Gaming
                {"Nintendo Switch 2", 550000, 100, 7},    // Gaming
                {"Dyson Airwrap", 650000, 80, 5},         // Wearable (lifestyle)
                {"Logitech MX Master", 150000, 200, 10},  // Mouse
                {"Logitech MX Keys", 130000, 200, 9},     // Keyboard
                {"Samsung Monitor 32", 550000, 90, 8},    // Monitor
                {"LG Monitor 27", 480000, 110, 8},        // Monitor
                {"Keychron K3 Pro", 140000, 150, 9},      // Keyboard
                {"Leopold FC750R", 180000, 100, 9},       // Keyboard
                {"Elgato Stream Deck", 200000, 70, 7},    // Gaming
                {"Raspberry Pi 5", 120000, 200, 7},       // Gaming
                {"Apple Magic Keyboard", 400000, 100, 9}, // Keyboard
                {"Apple Magic Trackpad", 180000, 120, 10},// Mouse
        };

        List<Product> products = new ArrayList<>();
        for (Object[] data : productData) {
            Product product = Product.createProduct((String) data[0], (int) data[1], (int) data[2]);
            categories.get((int) data[3]).addProduct(product);
            products.add(productRepository.save(product));
        }
        return products;
    }

    private void seedOrders(List<Member> members, List<Product> products) {
        Random random = new Random(42);
        String[] streets = {"123 Main St", "456 Oak Ave", "789 Pine Rd", "321 Elm Blvd", "654 Maple Dr"};
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix"};
        String[] zips = {"10001", "90001", "60601", "77001", "85001"};
        PaymentMethod[] methods = PaymentMethod.values();

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
                // Create delivery
                int addrIdx = random.nextInt(streets.length);
                Address address = new Address(zips[addrIdx], streets[addrIdx], cities[addrIdx]);
                Delivery delivery = Delivery.createDelivery(member.getName(), "010-" + (1000 + random.nextInt(9000)) + "-" + (1000 + random.nextInt(9000)), address);

                // Create payment
                int totalPrice = orderItems.stream().mapToInt(OrderItem::getTotalPrice).sum();
                Payment payment = Payment.createPayment(methods[random.nextInt(methods.length)], totalPrice);

                orderRepository.save(Order.createOrder(member, orderItems, delivery, payment));
            }
        }
    }

    private void seedCoupons(List<Member> members) {
        LocalDateTime now = LocalDateTime.now();

        // Active coupons
        Coupon welcome = Coupon.createCoupon("Welcome 10% Off", DiscountType.RATE, 10,
                1000, now.minusDays(30), now.plusDays(60), 10000);
        Coupon spring = Coupon.createCoupon("Spring Sale 5000 Off", DiscountType.FIXED, 5000,
                500, now.minusDays(7), now.plusDays(30), 30000);
        Coupon vip = Coupon.createCoupon("VIP 20% Off", DiscountType.RATE, 20,
                100, now.minusDays(1), now.plusDays(90), 50000);
        Coupon flash = Coupon.createCoupon("Flash Sale 15% Off", DiscountType.RATE, 15,
                50, now, now.plusHours(24), 20000);

        // Expired coupon
        Coupon expired = Coupon.createCoupon("Expired Coupon", DiscountType.FIXED, 3000,
                200, now.minusDays(60), now.minusDays(1), 10000);

        couponRepository.saveAll(List.of(welcome, spring, vip, flash, expired));

        // Issue coupons to first 10 members
        Random random = new Random(42);
        List<Coupon> activeCoupons = List.of(welcome, spring, vip, flash);
        for (int i = 0; i < 10; i++) {
            Member member = members.get(i);
            for (Coupon coupon : activeCoupons) {
                if (random.nextBoolean()) {
                    coupon.issue();
                    memberCouponRepository.save(MemberCoupon.issue(member, coupon));
                }
            }
        }

        log.info("[InitData] Coupons seeded - 5 coupons, issued to first 10 members");
    }
}
