# CLAUDE.md — Event Ticketing Platform

## Tổng quan hệ thống
Nền tảng bán vé sự kiện production-grade gồm 2 loại sự kiện: **ca nhạc (concert)** và **phim (movie)**.
Kiến trúc **microservice** với Spring WebFlux + Java 25.
Xem `PLANNING.md` để biết service nào đang được build và thứ tự ưu tiên.

---

## Tech Stack toàn hệ thống

| Thành phần | Công nghệ |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3.x + Spring WebFlux |
| Reactive lib | Project Reactor (`Mono`, `Flux`) |
| Service mesh | Spring Cloud Gateway, Eureka |
| Message queue | Apache Kafka |
| DB quan hệ | PostgreSQL + R2DBC |
| DB document | MongoDB Reactive |
| Cache / Lock | Redis Reactive |
| Search | Elasticsearch (Spring Data Elasticsearch) |
| Object storage | Cloudflare R2 (S3-compatible) |
| CDN | Cloudflare |
| Container | Docker + Docker Compose |
| Tracing | Micrometer + Zipkin |
| Config | Spring Cloud Config Server |
| Test | JUnit 5, StepVerifier, WebTestClient |

---

## Danh sách 8 Microservices

| Service | Port | DB | Mô tả |
|---|---|---|---|
| `api-gateway` | 8080 | — | Spring Cloud Gateway, routing, auth filter |
| `user-service` | 8081 | PostgreSQL | Đăng ký, đăng nhập, JWT, profile |
| `event-catalog-service` | 8082 | MongoDB | Thông tin concert, movie, venue, lineup |
| `booking-service` | 8083 | PostgreSQL | Đặt chỗ, seat locking, booking lifecycle |
| `payment-service` | 8084 | PostgreSQL | Thanh toán, hoàn tiền, tích hợp payment gateway |
| `notification-service` | 8085 | — | Email/SMS, consume Kafka events |
| `search-service` | 8086 | Elasticsearch | Full-text search, filter, suggest |
| `cms-service` | 8087 | MongoDB | Admin config homepage, banner, featured events |
| `media-service` | 8088 | PostgreSQL | Upload ảnh, resize, presigned URL, CDN invalidation |

---

## Cấu trúc Common Library

### Hai module dùng chung (thay cho 4 module cũ)

```
common/
├── common-entity/          # Pure domain — không có infrastructure dependency
│   └── com.ticketing.common
│       ├── dto/
│       │   ├── response/
│       │   │   ├── ApiResponse<T>      # Builder pattern — success/error factory methods
│       │   │   ├── ErrorResponse       # Error với FieldError record (validation)
│       │   │   └── PageResponse<T>     # Paginated wrapper — record (Java 25)
│       │   ├── request/
│       │   │   └── PageRequest         # Pagination query params record
│       │   ├── enums/
│       │   │   ├── BookingStatus       # PENDING→CONFIRMED→REFUNDED
│       │   │   ├── EventStatus         # DRAFT→PUBLISHED→ON_SALE→SOLD_OUT
│       │   │   ├── PaymentStatus       # PENDING→SUCCESS/FAILED→REFUNDED
│       │   │   └── TicketStatus        # AVAILABLE→RESERVED→SOLD
│       │   └── constants/
│       │       └── Constants           # RedisKeys, KafkaTopics, Headers, ErrorCodes
│       └── interfaces/
│           ├── Identifiable<ID>        # Marker interface — entity có ID
│           ├── mapper/
│           │   └── EntityMapper<E,D>   # Strategy/Adapter — MapStruct base interface
│           └── service/
│               └── CrudService<ID,REQ,RES>  # Template — reactive CRUD contract
│
└── common-base/            # Infrastructure lib — configs + exception + patterns
    └── com.ticketing.common
        ├── config/
        │   ├── redis/
        │   │   └── RedisConfig         # ReactiveRedisTemplate (String/String)
        │   ├── messaging/
        │   │   ├── KafkaConfig         # Producer (idempotent) + Consumer (manual ack)
        │   │   └── RabbitMQConfig      # Exchange, DLX, Queue, RabbitTemplate
        │   ├── web/
        │   │   └── WebFluxConfig       # WebClient.Builder + CorrelationId filter
        │   └── openapi/
        │       └── OpenApiAutoConfig   # JWT Bearer scheme, @ConditionalOnClass
        ├── exception/
        │   ├── TicketingException      # Base — mang HttpStatus, không cần switch-case
        │   ├── domain/
        │   │   ├── ResourceNotFoundException   # 404 generic
        │   │   ├── ConflictException           # 409
        │   │   ├── UnauthorizedException       # 401
        │   │   ├── PaymentException            # 402
        │   │   └── BookingExpiredException     # 409 (booking cụ thể)
        │   └── handler/
        │       └── GlobalExceptionHandler      # @RestControllerAdvice — đọc httpStatus từ exception
        ├── messaging/                  # Observer pattern
        │   ├── DomainEvent<T>          # Wrapper record — eventId, eventType, payload, occurredAt
        │   ├── EventPublisher<E>       # Interface — publish(topic, event) / publish(topic, key, event)
        │   ├── EventConsumer<E>        # Interface — consume(event) — implement = idempotent
        │   └── kafka/
        │       └── KafkaEventPublisher<E>  # Impl — KafkaTemplate + subscribeOn(boundedElastic)
        ├── cache/                      # Strategy pattern
        │   ├── CacheStrategy<K,V>      # Interface — get/put/evict/getOrLoad
        │   └── redis/
        │       └── RedisCacheStrategy<V>   # Redis impl — JSON serialize
        ├── lock/                       # Strategy pattern
        │   ├── DistributedLock         # Interface — acquire/release/executeWithLock
        │   └── redis/
        │       └── RedisDistributedLock    # Lua script atomic release
        ├── ratelimit/                  # Strategy pattern
        │   ├── RateLimiter             # Interface — isAllowed/checkOrThrow
        │   └── redis/
        │       └── RedisRateLimiter        # Sliding Window Log — Redis Sorted Set
        └── resources/META-INF/spring/
            └── AutoConfiguration.imports   # Spring Boot auto-load tất cả configs
```

### Quy tắc dependency giữa common modules

```
common-entity   ←── common-base     (base phụ thuộc entity)
common-entity   ←── api-gateway     (chỉ cần DTOs/constants, KHÔNG cần infra)
common-entity   ←┐
common-base     ←┤── mọi service còn lại (user, event, booking, payment...)
```

### Design Patterns trong Common

| Pattern | Class | Mục đích |
|---|---|---|
| **Builder** | `ApiResponse`, `ErrorResponse` | Fluent construction với factory methods |
| **Strategy** | `CacheStrategy`, `DistributedLock`, `RateLimiter` | Swap Redis ↔ in-memory dễ dàng cho test |
| **Observer** | `EventPublisher`, `EventConsumer`, `DomainEvent` | Kafka/RabbitMQ abstraction |
| **Template Method** | `CrudService` | Contract chuẩn cho CRUD service |
| **Adapter** | `EntityMapper` | MapStruct base interface |

### Cách service mới dùng common

**`pom.xml`** — thêm 2 dependency:
```xml
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-entity</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-base</artifactId>
    <version>${project.version}</version>
</dependency>
```

**`application.yml`** — configure OpenAPI title:
```yaml
common:
  openapi:
    title: "Booking Service API"
    description: "Seat locking, booking lifecycle, ticket issuance"
```

**Service code** — dùng patterns:
```java
// Cache-aside
return cacheStrategy.getOrLoad("event:" + id, Duration.ofMinutes(30),
    () -> eventRepository.findById(id).map(mapper::toDto));

// Distributed lock khi book seat
return distributedLock.executeWithLock("booking:seat:" + seatId, Duration.ofSeconds(10),
    bookingService.createBooking(userId, seatId));

// Rate limiting
return rateLimiter.checkOrThrow("booking:" + userId, 10, Duration.ofMinutes(1))
    .then(bookingService.createBooking(request));

// Publish event
return bookingRepository.save(booking)
    .flatMap(saved -> eventPublisher.publish(
        KafkaTopics.BOOKING_CREATED,
        saved.getId().toString(),
        DomainEvent.of("booking.created", "booking-service", new BookingCreatedPayload(saved))))
    .map(mapper::toResponse);
```

---

## Cấu trúc thư mục (mỗi service theo cùng pattern)

```
{service-name}/
├── src/main/java/com/ticketing/{service}/
│   ├── domain/
│   │   ├── model/          # Entity, record classes, sealed interfaces
│   │   └── repository/     # Reactive repository interfaces
│   ├── application/
│   │   └── service/        # Business logic — Mono<T> / Flux<T>
│   ├── api/
│   │   ├── handler/        # WebFlux functional handlers
│   │   ├── router/         # RouterFunction definitions
│   │   └── dto/            # Request/Response records
│   ├── infrastructure/
│   │   ├── config/         # DB, Security configs
│   │   ├── adapter/        # External service clients (WebClient)
│   │   └── messaging/      # Kafka consumers — implement EventConsumer<T>
│   └── common/
│       └── exception/      # Service-specific exceptions (extend TicketingException)
├── src/test/
├── src/main/resources/
│   ├── application.yml
│   └── schema.sql          # R2DBC: tạo bảng thủ công
└── Dockerfile
```

---

## Domain Model

### Các entity chính

```
── User ──────────────────────────────────────────────────────
   id (UUID), email, passwordHash
   firstName, lastName, phoneNumber
   dateOfBirth, profileImageUrl
   emailVerified (Boolean)
   role: CUSTOMER | ADMIN
   status: PENDING_VERIFICATION | ACTIVE | INACTIVE | SUSPENDED | DELETED
   createdAt, updatedAt, lastLoginAt, deletedAt (soft delete)

── UserPreferences ───────────────────────────────────────────
   id (UUID), userId (FK → User)
   emailNotifications, smsNotifications, pushNotifications (Boolean)
   preferredLanguage, timezone, currency
   marketingEmails (Boolean)

── VerificationToken ─────────────────────────────────────────
   id (UUID), userId (FK → User)
   token, tokenType: EMAIL_VERIFICATION | PASSWORD_RESET
   expiresAt, usedAt, createdAt

── Event (MongoDB) ───────────────────────────────────────────
   id, title, type: CONCERT | MOVIE
   venueId, startTime, endTime
   status: DRAFT | ON_SALE | SOLD_OUT | CANCELLED | ENDED
   detail: ConcertDetail | MovieDetail  ← sealed interface

── ConcertDetail ─────────────────────────────────────────────
   artists[], genres[], ageRestriction

── MovieDetail ───────────────────────────────────────────────
   director, cast[], genre, duration, format: 2D | 3D | IMAX
   rating: G | PG | PG13 | R

── Venue ─────────────────────────────────────────────────────
   id, name, address, city, capacity, zones[]

── Seat ──────────────────────────────────────────────────────
   id, eventId, code (A1, B12...), zone, price
   status: AVAILABLE | LOCKED | SOLD

── Booking ───────────────────────────────────────────────────
   id, userId, eventId
   status: PENDING | PAID | CANCELLED | REFUNDED
   createdAt, expiredAt  ← PENDING hết hạn sau 10 phút

── BookingItem ───────────────────────────────────────────────
   id, bookingId, seatId, priceSnapshot

── Payment ───────────────────────────────────────────────────
   id, bookingId, amount, currency: VND
   method: VNPAY | MOMO | STRIPE
   status: PENDING | COMPLETED | FAILED | REFUNDED
   transactionId, paidAt

── Ticket ────────────────────────────────────────────────────
   id, bookingItemId, qrCode, isUsed, usedAt
```

### Event sealed interface (Java 25)
```java
public sealed interface EventDetail
    permits ConcertDetail, MovieDetail {}

public record ConcertDetail(
    List<String> artists,
    List<String> genres,
    Integer ageRestriction
) implements EventDetail {}

public record MovieDetail(
    String director,
    List<String> cast,
    MovieFormat format,
    Integer durationMinutes,
    AgeRating rating
) implements EventDetail {}
```

### Booking status flow
```
PENDING ──(thanh toán thành công)──► PAID ──(admin)──► REFUNDED
PENDING ──(hết 10 phút / user huỷ)──► CANCELLED
PAID    ──(admin force)──────────────► CANCELLED
```

---

## Business Rules — Quan trọng, không được bỏ qua

### Booking & Seat
- Seat locking tối đa **10 phút** — dùng Redis TTL, tự động release khi hết hạn
- Một user mua tối đa **4 vé / sự kiện**
- Không thể book seat của event có status `CANCELLED` hoặc `ENDED`
- `BookingItem.priceSnapshot` — snapshot giá tại thời điểm đặt, không thay đổi dù giá event thay đổi sau

### Payment
- Vé đã `PAID` **không hoàn tiền tự động** — chỉ ADMIN mới trigger được refund
- Payment timeout: **15 phút** kể từ khi tạo Booking
- Luôn verify webhook signature từ payment gateway trước khi xử lý

### Phân quyền
- `CUSTOMER`: chỉ xem event, tạo booking, xem vé của chính mình
- `ADMIN`: full access, thêm/sửa event, quản lý refund, CMS config
- API Gateway validate JWT và forward `X-User-Id`, `X-User-Role` header xuống các service

### Media
- File upload tối đa **10MB**, chỉ chấp nhận `image/jpeg`, `image/png`, `image/webp`
- Resize về 3 kích thước chuẩn: thumbnail (300×200), card (800×533), banner (1920×640)
- URL public luôn qua CDN: `https://cdn.ticketing.com/events/{eventId}/{size}.webp`

---

## Kafka Topics

| Topic | Producer | Consumer | Payload |
|---|---|---|---|
| `booking.created` | booking-service | notification-service | bookingId, userId, eventId |
| `booking.expired` | booking-service | notification-service | bookingId |
| `payment.completed` | payment-service | booking-service, notification-service | paymentId, bookingId |
| `payment.failed` | payment-service | booking-service, notification-service | paymentId, reason |
| `ticket.issued` | booking-service | notification-service | ticketId, qrCode |
| `event.published` | event-catalog-service | search-service, cms-service | eventId |
| `event.updated` | event-catalog-service | search-service | eventId |
| `media.uploaded` | media-service | event-catalog-service | mediaId, url, eventId |

---

## API Design

### Response format nhất quán (tất cả services)
```json
// Success — single object
{ "data": { ... }, "meta": { "timestamp": "2025-01-01T00:00:00Z" } }

// Success — paginated list
{
  "data": [...],
  "meta": { "page": 1, "limit": 20, "total": 150, "timestamp": "..." }
}

// Error
{
  "error": {
    "code": "SEAT_ALREADY_LOCKED",
    "message": "Ghế A1 đang được người khác giữ",
    "traceId": "abc123"
  }
}
```

### URL conventions
- Resource dạng số nhiều: `/api/v1/events`, `/api/v1/bookings`
- Nested resource: `/api/v1/events/{id}/seats`
- Action không phải CRUD dùng verb: `/api/v1/bookings/{id}/cancel`
- Search endpoint: `/api/v1/search/events?q=coldplay&type=CONCERT&city=hanoi`

---

## Reactive Programming Rules

### Tuyệt đối không làm
```java
// ❌ block() trong pipeline — gây deadlock trên Netty thread
ticketService.findById(id).block();

// ❌ subscribe() thủ công trong application code
bookingService.save(booking).subscribe();

// ❌ try/catch trong reactive pipeline
try { return eventService.findById(id); }
catch (Exception e) { ... }

// ❌ Optional trong reactive chain
Optional<Event> opt = eventRepository.findById(id).block();
```

### Cách đúng
```java
// ✅ Chain tiếp, không block
return seatRepository.findById(seatId)
    .switchIfEmpty(Mono.error(new SeatNotFoundException(seatId)))
    .filter(seat -> seat.status() == SeatStatus.AVAILABLE)
    .switchIfEmpty(Mono.error(new SeatNotAvailableException(seatId)))
    .flatMap(seat -> lockSeatInRedis(seat))
    .flatMap(seat -> bookingRepository.save(buildBooking(userId, seat)))
    .map(bookingMapper::toDto);

// ✅ Error handling trong pipeline
.onErrorMap(DataAccessException.class,
    e -> new ServiceException("DB error", e))

// ✅ map vs flatMap
.map(event -> new EventDto(event))           // transform → type thường
.flatMap(event -> venueService.findById(...)) // transform → Mono/Flux
```

### Java 25 — luôn dùng
```java
// Record cho DTO
public record CreateBookingRequest(Long eventId, List<Long> seatIds) {}

// Switch expression
String status = switch (booking.status()) {
    case PENDING  -> "Chờ thanh toán";
    case PAID     -> "Đã thanh toán";
    case CANCELLED -> "Đã huỷ";
    case REFUNDED -> "Đã hoàn tiền";
};

// Text block cho query dài
String query = """
    SELECT b.*, u.email
    FROM bookings b
    JOIN users u ON b.user_id = u.id
    WHERE b.status = :status
    AND b.expired_at < :now
    """;
```

---

## Coding Standards

### Hai phong cách Web layer — chọn theo từng service

Dự án dùng **Spring WebFlux** cho tất cả service (reactive, non-blocking). Tuy nhiên WebFlux hỗ trợ 2 phong cách viết controller — chọn 1 trong 2 cho mỗi service và **giữ nhất quán trong service đó**.

---

#### Phong cách 1 — Annotation-based (`@RestController`)
Dùng cho service **CRUD đơn giản**, ít routing logic phức tạp.
`user-service` dùng phong cách này.

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> register(
            @Valid @RequestBody RegisterRequest request) {
        return userService.register(request)
                .map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(user)));
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<UserResponse>>> getUserById(
            @PathVariable UUID userId) {
        return userService.getUserById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)));
    }
}
```

---

#### Phong cách 2 — Functional routing (`RouterFunction` + `Handler`)
Dùng cho service có **routing phức tạp**, nhiều filter, hoặc cần kiểm soát request/response ở mức thấp hơn.
`booking-service`, `payment-service`, `api-gateway` nên dùng phong cách này.

```java
// Router
@Bean
public RouterFunction<ServerResponse> bookingRoutes(BookingHandler handler) {
    return route()
        .POST("/api/v1/bookings", handler::handleCreate)
        .GET("/api/v1/bookings/{id}", handler::handleGetById)
        .POST("/api/v1/bookings/{id}/cancel", handler::handleCancel)
        .build();
}

// Handler
public Mono<ServerResponse> handleCreate(ServerRequest request) {
    return request.bodyToMono(CreateBookingRequest.class)
        .flatMap(bookingService::createBooking)
        .flatMap(dto -> ServerResponse.status(CREATED).bodyValue(dto));
}
```

---

#### Phân công các service

| Service | Phong cách | Lý do |
|---|---|---|
| `user-service` | `@RestController` | CRUD profile, auth — đơn giản |
| `event-catalog-service` | `@RestController` | CRUD event/venue |
| `booking-service` | Functional | Routing phức tạp, seat locking, nhiều action endpoint |
| `payment-service` | Functional | Webhook handling, signature verify cần filter |
| `search-service` | `@RestController` | Query đơn giản |
| `cms-service` | `@RestController` | Admin CRUD config |
| `media-service` | Functional | Multipart upload cần xử lý request thủ công |
| `notification-service` | Không có HTTP controller | Chỉ consume Kafka |
| `api-gateway` | Functional (Spring Cloud Gateway) | Route + filter JWT |

---

### Đặt tên nhất quán

**Annotation-based:**
- Controller method: `register`, `login`, `getUserById`, `updateProfile`
- Service: `register`, `login`, `findById`, `updateProfile`

**Functional:**
- Handler method: `handleCreate`, `handleGetById`, `handleCancel`, `handleListByUser`
- Service: `createBooking`, `findById`, `cancelBooking`, `releaseExpiredBookings`

**Chung:**
- Kafka producer: `publishBookingCreated`, `publishPaymentCompleted`
- Kafka consumer: `onBookingCreated`, `onPaymentCompleted`

---

## Testing

### Bộ 3 công cụ bắt buộc
- **StepVerifier** — test mọi Mono/Flux trong service layer
- **WebTestClient** — integration test HTTP endpoint
- **EmbeddedKafka** — test Kafka producer/consumer

```java
// StepVerifier — service test
@Test
void shouldFailWhenSeatAlreadyLocked() {
    StepVerifier.create(bookingService.createBooking(userId, lockedSeatId))
        .expectError(SeatNotAvailableException.class)
        .verify();
}

// WebTestClient — handler test
@Test
void shouldReturn201WhenBookingCreated() {
    webTestClient.post().uri("/api/v1/bookings")
        .bodyValue(new CreateBookingRequest(eventId, List.of(seatId)))
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.data.status").isEqualTo("PENDING");
}
```

---

## Hướng dẫn cho Claude khi làm việc

1. **Luôn hỏi** trước khi thay đổi cấu trúc thư mục hoặc cài dependency mới
2. **Giải thích "tại sao"** khi dùng operator reactive không quen — đặc biệt `flatMap`, `switchIfEmpty`, `zipWith`
3. **So sánh với Spring MVC** khi hữu ích để thấy sự khác biệt
4. **Chỉ ra ngay** nếu code có nguy cơ block Netty thread
5. **Gợi ý Kafka topic** nếu một action trong service nên trigger event sang service khác
6. Khi viết test, luôn dùng **StepVerifier** — không dùng `.block()` trong test

---

## Lệnh hay dùng

```bash
# Chạy toàn bộ infrastructure (Kafka, Redis, Postgres, MongoDB, Elasticsearch)
docker compose up -d

# Chạy một service cụ thể
cd booking-service && ./mvnw spring-boot:run

# Chạy test một service
./mvnw test -pl booking-service

# Chạy test một class cụ thể
./mvnw test -pl booking-service -Dtest=BookingServiceTest

# Build tất cả service
./mvnw package -DskipTests

# Xem log Kafka topic
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic booking.created --from-beginning
```

---

*Cập nhật file này khi: thêm service mới, thay đổi business rule, thêm Kafka topic, hoặc quyết định kiến trúc quan trọng.*
*Xem `PLANNING.md` để biết service và feature đang được phát triển.*