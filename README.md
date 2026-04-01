# Java Spring Boot

A repository of core Java + Spring Boot features through hands-on implementation.

## Tech Stack

- Java 21
- Spring Boot 4.0.5
- Spring Data JPA / Hibernate
- PostgreSQL 17 (Docker)
- H2 (Test)
- Lombok
- Gradle 9.4

## Order Service

A domain for practicing JPA relationships and solving the N+1 problem.

### Entity Relationships

```
Member (1) ---- (N) Order (1) ---- (N) OrderItem (N) ---- (1) Product
```

### Practice Topics

| Topic | Description | Location |
|-------|-------------|----------|
| Fetch Join | JPQL fetch join queries to solve N+1 problem | `OrderRepository` |
| Cascade | Auto persist/remove OrderItems with Order (`CascadeType.ALL`) | `Order.orderItems` |
| Bidirectional Convenience Methods | Set both sides of a relationship in one call | `Order.setMember()`, `Order.addOrderItem()` |
| Order Creation | Static factory method + stock deduction | `Order.createOrder()`, `OrderItem.createOrderItem()` |
| Order Cancellation | Status change + stock recovery | `Order.cancel()`, `OrderItem.cancel()` |
| Total Price Calculation | Stream-based aggregation | `Order.getTotalPrice()` |

### Project Structure

```
src/main/java/com/example/order/
├── domain/          # Entities (Member, Product, Order, OrderItem, OrderStatus)
├── repository/      # Spring Data JPA + Fetch Join JPQL
├── service/         # Business logic
├── dto/             # Request/Response DTOs
├── controller/      # REST API
├── InitData.java    # Sample seed data
└── OrderApplication.java
```

### API

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/orders` | Create order |
| POST | `/api/orders/{id}/cancel` | Cancel order |
| GET | `/api/orders/{id}` | Get single order |
| GET | `/api/orders` | Get all orders |
| GET | `/api/orders/members/{memberId}` | Get orders by member |

## Run

```bash
# Start PostgreSQL
docker compose up -d

# Run application
./gradlew bootRun

# Run tests (H2 in-memory)
./gradlew test
```
