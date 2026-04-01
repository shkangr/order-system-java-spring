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

## Entity Relationships

```
                          Category (self-referencing parent/children)
                              |
                         (N) Product (1) ---- (N) OrderItem (N) ---- (1) Order
                                                                          |
                                                    Member (1) ---- (N) Order
                                                                          |
                                                   Delivery (1) ---- (1) Order
                                                    Payment (1) ---- (1) Order
```

## Order Service

### Practice Topics

| Topic | Description | Location |
|-------|-------------|----------|
| Fetch Join (QueryDSL) | Type-safe fetch join queries to solve N+1 problem | `OrderRepositoryCustomImpl` |
| Pagination | QueryDSL offset/limit + batch_fetch_size for ToMany | `OrderRepositoryCustomImpl.findAllWithMember()` |
| Cascade | Auto persist/remove OrderItems, Delivery, Payment with Order | `Order` entity |
| @OneToOne | Order ↔ Delivery, Order ↔ Payment | `Order`, `Delivery`, `Payment` |
| @Embeddable | Address value type (zipCode, address1, address2) | `Address`, `Delivery` |
| Bidirectional Convenience Methods | Set both sides of a relationship in one call | `Order`, `Category` |
| Order Creation | Factory method + stock deduction + delivery + payment | `Order.createOrder()` |
| Order Cancellation | Validates delivery status, restores stock, cancels delivery/payment | `Order.cancel()` |
| Total Price Calculation | Stream-based aggregation | `Order.getTotalPrice()` |

## Delivery Service

| Feature | Description |
|---------|-------------|
| Delivery Status Flow | `READY → SHIPPING → COMPLETED` |
| Cancel Restriction | Order cannot be cancelled if delivery is `SHIPPING` or `COMPLETED` |
| Cascade | Delivery is auto-saved when Order is saved (`CascadeType.ALL`) |

## Payment Service

| Feature | Description |
|---------|-------------|
| Payment Status Flow | `READY → PAID → CANCELED` or `READY → FAILED` |
| Payment Methods | `CARD`, `BANK_TRANSFER`, `POINT` |
| Cancel Sync | When order is cancelled, paid payments are automatically canceled |
| Payment Key | Auto-generated UUID on creation |

## Category Service

| Feature | Description |
|---------|-------------|
| Hierarchical Structure | Self-referencing `parent` / `children` relationship |
| Product Mapping | `Category (1) → (N) Product` |
| Tree API | Root categories with nested children response |

### @Async - Slack Notification

When an order is created or cancelled, a Slack notification is sent asynchronously on a separate thread pool, so the API response is not blocked by external I/O.

**Problem without @Async**

```
Client Request
  → createOrder()        (10ms)
  → sendSlackMessage()   (1000ms+, network I/O)
  → Response             (total: 1010ms+)
```

**Solution with @Async**

```
Client Request
  → createOrder()        (10ms)
  → Response             (total: 10ms)

  [notification-1 thread]
  → sendSlackMessage()   (runs independently)
```

**Custom ThreadPoolTaskExecutor**

| Property | Value | Purpose |
|----------|-------|---------|
| corePoolSize | 2 | Minimum threads kept alive |
| maxPoolSize | 5 | Maximum threads under load |
| queueCapacity | 50 | Pending tasks before creating new threads |
| threadNamePrefix | `notification-` | Easy to identify in logs |

**AsyncUncaughtExceptionHandler**

If the async method throws an exception (e.g. Slack API failure), it cannot propagate back to the caller since it runs on a different thread. `AsyncUncaughtExceptionHandler` catches these and logs the error with method name, parameters, and stack trace, preventing silent failures.

### Global Exception Handler

Centralized error handling with `@RestControllerAdvice`.

**Response format**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Order not found. id=999",
  "timestamp": "2026-04-01T16:00:00"
}
```

**Exception → Status Code mapping**

| Status | Exception | When |
|--------|-----------|------|
| 400 | `MethodArgumentNotValidException` | `@Valid` fails (null, empty, negative count) |
| 400 | `IllegalArgumentException` | Invalid request parameter |
| 404 | `EntityNotFoundException` (custom) | Member, Product, Order, Delivery, Payment, Category not found |
| 404 | `NoResourceFoundException` | Unknown API path |
| 409 | `IllegalStateException` | Not enough stock, already cancelled, invalid delivery/payment status transition |
| 500 | `Exception` | Unexpected server error |

### Project Structure

```
src/main/java/com/example/order/
├── domain/          # Entities, Enums, Embeddable
│   ├── Member, Product, Order, OrderItem, OrderStatus
│   ├── Delivery, DeliveryStatus, Address (@Embeddable)
│   ├── Payment, PaymentStatus, PaymentMethod
│   └── Category
├── repository/      # Spring Data JPA + QueryDSL Fetch Join
├── config/          # QueryDSL config, Async thread pool config
├── exception/       # Custom exceptions (EntityNotFoundException)
├── service/         # OrderService, DeliveryService, PaymentService, CategoryService
├── dto/             # Request/Response DTOs
├── controller/      # REST API controllers
├── InitData.java    # Seed data (30 members, 30 products, 14 categories, 300 orders)
└── OrderApplication.java
```

### API

**Orders**

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/orders` | Create order (with delivery + payment) |
| POST | `/api/orders/{id}/cancel` | Cancel order |
| GET | `/api/orders/{id}` | Get single order |
| GET | `/api/orders?page=0&size=10` | Get orders (paginated) |
| GET | `/api/orders/members/{memberId}` | Get orders by member |

**Deliveries**

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/deliveries/{id}` | Get delivery info |
| POST | `/api/deliveries/{id}/ship` | Start shipping |
| POST | `/api/deliveries/{id}/complete` | Complete delivery |

**Payments**

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/payments/{id}` | Get payment info |
| POST | `/api/payments/{id}/approve` | Approve payment |
| POST | `/api/payments/{id}/fail` | Fail payment |

**Categories**

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/categories` | Create category |
| GET | `/api/categories` | Get all categories (tree) |
| GET | `/api/categories/{id}` | Get single category |

## Run

```bash
# Start PostgreSQL
docker compose up -d

# Run application
./gradlew bootRun

# Run tests (H2 in-memory)
./gradlew test
```
