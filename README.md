# Java Spring Boot

A repository of core Java + Spring Boot features through hands-on implementation.

## Tech Stack

- Java 21
- Spring Boot 4.0.5
- Spring Data JPA / Hibernate
- PostgreSQL 17 (Docker)
- H2 (Test)
- QueryDSL 5.1.0
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
| Fetch Join (QueryDSL) | Type-safe fetch join queries to solve N+1 problem | `OrderRepositoryCustomImpl` |
| Cascade | Auto persist/remove OrderItems with Order (`CascadeType.ALL`) | `Order.orderItems` |
| Bidirectional Convenience Methods | Set both sides of a relationship in one call | `Order.setMember()`, `Order.addOrderItem()` |
| Order Creation | Static factory method + stock deduction | `Order.createOrder()`, `OrderItem.createOrderItem()` |
| Order Cancellation | Status change + stock recovery | `Order.cancel()`, `OrderItem.cancel()` |
| Total Price Calculation | Stream-based aggregation | `Order.getTotalPrice()` |

### @Async - Slack Notification

When an order is created or cancelled, a Slack notification is sent asynchronously on a separate thread pool, so the API response is not blocked by external I/O.

**Problem without @Async**

```
Client Request
  → createOrder()        (10ms)
  → sendSlackMessage()   (1000ms+, network I/O)
  → Response             (total: 1010ms+)
```

The client waits for the Slack API call to finish, even though the order itself is already complete.

**Solution with @Async**

```
Client Request
  → createOrder()        (10ms)
  → Response             (total: 10ms)

  [notification-1 thread]
  → sendSlackMessage()   (runs independently)
```

The Slack call runs on a separate `notification-` thread pool. The client gets an immediate response.

**Custom ThreadPoolTaskExecutor**

| Property | Value | Purpose |
|----------|-------|---------|
| corePoolSize | 2 | Minimum threads kept alive |
| maxPoolSize | 5 | Maximum threads under load |
| queueCapacity | 50 | Pending tasks before creating new threads |
| threadNamePrefix | `notification-` | Easy to identify in logs |

**AsyncUncaughtExceptionHandler**

If the async method throws an exception (e.g. Slack API failure), it cannot propagate back to the caller since it runs on a different thread. `AsyncUncaughtExceptionHandler` catches these and logs the error with method name, parameters, and stack trace, preventing silent failures.

| File | Role |
|------|------|
| `AsyncConfig` | Thread pool config + exception handler |
| `SlackNotificationService` | `@Async` Slack message sender |
| `OrderService` | Calls notification after order create/cancel |

### Global Exception Handler

Centralized error handling with `@RestControllerAdvice`. All exceptions thrown from Controllers are caught and converted to a consistent JSON response.

**Response format**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "존재하지 않는 주문입니다. id=999",
  "timestamp": "2026-04-01T16:00:00"
}
```

**Exception → Status Code mapping**

| Status | Exception | When |
|--------|-----------|------|
| 400 | `MethodArgumentNotValidException` | `@Valid` fails (null, empty, negative count) |
| 400 | `IllegalArgumentException` | Invalid request parameter |
| 404 | `EntityNotFoundException` (custom) | Member, Product, or Order not found |
| 404 | `NoResourceFoundException` | Unknown API path (e.g. `GET /api/nothing`) |
| 409 | `IllegalStateException` | Not enough stock, already cancelled order |
| 500 | `Exception` | Unexpected server error |

| File | Role |
|------|------|
| `GlobalExceptionHandler` | `@RestControllerAdvice` — catches and maps all exceptions |
| `EntityNotFoundException` | Custom exception for entity lookup failures (→ 404) |

### Project Structure

```
src/main/java/com/example/order/
├── domain/          # Entities (Member, Product, Order, OrderItem, OrderStatus)
├── repository/      # Spring Data JPA + QueryDSL Fetch Join
├── config/          # QueryDSL config, Async thread pool config
├── exception/       # Custom exceptions (EntityNotFoundException)
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
