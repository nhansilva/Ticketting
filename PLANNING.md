# PLANNING.md — Event Ticketing Platform

> Cập nhật: 2026-04-11
> Trạng thái tổng thể: **Phase 1 — user-service + api-gateway hoàn thành ✅, tiếp theo: event-catalog-service**

---

## Tình trạng hiện tại

| Service | Trạng thái | Ghi chú |
|---|---|---|
| `user-service` | 🟢 Done | Hoàn thành 2026-04-11 |
| `api-gateway` | 🟢 Done | Hoàn thành 2026-04-11 |
| `event-catalog-service` | 🔴 Chưa bắt đầu | — |
| `booking-service` | 🔴 Chưa bắt đầu | — |
| `payment-service` | 🔴 Chưa bắt đầu | — |
| `notification-service` | 🔴 Chưa bắt đầu | — |
| `search-service` | 🔴 Chưa bắt đầu | — |
| `cms-service` | 🔴 Chưa bắt đầu | — |
| `media-service` | 🔴 Chưa bắt đầu | — |
| `common` | 🟢 Done | dto, exception, config, utils |

---

## Phase 1 — Core Foundation (ưu tiên cao nhất)

### 1.1 Hoàn thiện `user-service` ✅ Hoàn thành 2026-04-11

- [x] Implement `refreshToken` — validate claim `type=refresh`, generate cặp token mới
- [x] Fix `verifyEmail` + `resetPassword` (đang `return null`)
- [x] Uncomment `findValidToken` trong `VerificationTokenRepository`
- [x] Fix `JwtAuthenticationFilter` — dùng role thực từ token thay vì hardcode `ROLE_USER`
- [x] `AdminDataSeeder` — tự seed `admin@ticketing.com` lúc startup
- [x] Endpoint `POST /api/v1/users/admin/create` — chỉ `ROLE_ADMIN` mới gọi được
- [x] `SecurityConfig` — thêm rule `hasRole("ADMIN")` cho `/admin/**`
- [x] `UserServiceTest` — 14 test cases (StepVerifier): register, login, refreshToken, verifyEmail, changePassword, getUserById
- [x] `UserControllerTest` — 9 test cases (WebTestClient): HTTP status, `@WithMockUser`

---

### 1.2 Build `api-gateway` (Port 8080)

✅ Hoàn thành 2026-04-11

- [x] `pom.xml` — spring-cloud-starter-gateway, Redis, JWT, common-dto/exception
- [x] `JwtAuthFilter` — validate JWT, inject `X-User-Id` + `X-User-Role`, public routes bypass
- [x] `RateLimitFilter` — Sliding Window Log với Redis Sorted Set, 2 mode PER_USER/PER_IP, fail open
- [x] `CorrelationIdFilter` — GlobalFilter inject `X-Correlation-Id` + `X-Request-Id` vào mọi request
- [x] `GatewayErrorWebExceptionHandler` — format `{"error": {"code","message","traceId"}}`, Retry-After header
- [x] `GatewayConfig` — functional DSL routes cho 7 services, payment webhook route riêng (no JWT)
- [x] `SecurityConfig` — tắt Spring Security defaults để tránh conflict với JwtAuthFilter
- [x] `application.yml` — service URLs qua env vars, CORS, httpclient timeouts
- [x] `JwtAuthFilterTest` — 7 test cases (public routes, valid token, expired, tampered, ADMIN role)
- [x] `RateLimitFilterTest` — 5 test cases (under limit, over limit, fail open, PER_IP, fallback to IP)
- [x] Đã fix Spring Boot 4.x → 3.4.4 (Spring Cloud 2024.0.x không tương thích Boot 4.x)
- [x] Mockito subclass mock maker cho Java 25 compatibility
- [x] GlobalExceptionHandler — thêm user error codes, import vào UserControllerTest

---

### 1.3 Build `event-catalog-service` (Port 8082)

Nền tảng cho booking. Dùng **MongoDB Reactive + @RestController**.

- [ ] Setup MongoDB Reactive, ánh xạ domain:
  - `Event` document với `sealed interface EventDetail` (ConcertDetail / MovieDetail)
  - `Venue` document
- [ ] CRUD endpoints:
  - `POST /api/v1/events` (ADMIN)
  - `GET /api/v1/events/{id}` (public)
  - `GET /api/v1/events?type=CONCERT&city=hanoi&page=0&size=20` (public, paginated)
  - `PUT /api/v1/events/{id}` (ADMIN)
  - `PUT /api/v1/events/{id}/status` (ADMIN — publish, cancel)
  - `GET /api/v1/venues`, `POST /api/v1/venues` (ADMIN)
- [ ] Seat management:
  - `GET /api/v1/events/{id}/seats`
  - Khi event được tạo → auto-generate seats theo venue zones
- [ ] Kafka producer: publish `event.published`, `event.updated` sau mỗi thay đổi
- [ ] Test: CRUD event, filter theo type/city, seat generation

---

## Phase 2 — Core Business

### 2.1 Build `booking-service` (Port 8083)

Service phức tạp nhất. Dùng **functional routing**.

- [ ] Domain: `Booking`, `BookingItem`, `Ticket`
- [ ] Redis seat locking — TTL 10 phút:
  - `SETNX booking:seat:{seatId} {userId}` với TTL 600s
  - Scheduler release expired locks
- [ ] Booking flow:
  - `POST /api/v1/bookings` — lock seats → tạo Booking `PENDING`
  - `POST /api/v1/bookings/{id}/cancel` — release lock, đổi status `CANCELLED`
  - `GET /api/v1/bookings/{id}` — chỉ owner hoặc ADMIN
  - `GET /api/v1/bookings/my` — list của current user
- [ ] Business rules:
  - Tối đa 4 vé / user / event
  - Không book nếu event `CANCELLED` hoặc `ENDED`
  - `BookingItem.priceSnapshot` — snapshot giá tại thời điểm đặt
- [ ] Kafka:
  - Consume `payment.completed` → đổi Booking sang `PAID`, issue Ticket, publish `ticket.issued`
  - Consume `payment.failed` → đổi sang `CANCELLED`, release lock
  - Publish `booking.created`, `booking.expired`
- [ ] Test: seat locking, max 4 vé, expire flow

---

### 2.2 Build `payment-service` (Port 8084)

Dùng **functional routing** (webhook cần filter để verify signature).

- [ ] Domain: `Payment`
- [ ] Endpoints:
  - `POST /api/v1/payments/initiate` — tạo Payment `PENDING`, trả về payment URL
  - `POST /api/v1/payments/webhook/vnpay` — nhận callback từ VNPay
  - `POST /api/v1/payments/webhook/momo` — nhận callback từ MoMo
  - `POST /api/v1/payments/{id}/refund` (ADMIN only)
  - `GET /api/v1/payments/{bookingId}`
- [ ] Webhook filter: verify HMAC signature trước khi vào handler
- [ ] Payment timeout: 15 phút — Kafka delayed event hoặc scheduler
- [ ] Kafka: publish `payment.completed`, `payment.failed`
- [ ] **Lưu ý**: Dùng mock payment gateway cho dev/test, cấu hình real keys qua env

---

### 2.3 Build `notification-service` (Port 8085)

Chỉ consume Kafka, không có HTTP controller.

- [ ] Kafka consumers:
  - `booking.created` → Email "Đặt chỗ thành công, chờ thanh toán"
  - `booking.expired` → Email "Booking đã hết hạn"
  - `payment.completed` → Email "Thanh toán thành công, vé đính kèm"
  - `payment.failed` → Email "Thanh toán thất bại"
  - `ticket.issued` → Email đính kèm QR code vé
- [ ] Email template: HTML Thymeleaf hoặc Freemarker
- [ ] SMTP config: Mailgun hoặc SendGrid (cấu hình qua env)
- [ ] Test: EmbeddedKafka, assert email content

---

## Phase 3 — Enhancement

### 3.1 Build `search-service` (Port 8086)

- [ ] Elasticsearch index `events`
- [ ] Consume Kafka `event.published`, `event.updated` → upsert vào Elasticsearch
- [ ] Endpoint: `GET /api/v1/search/events?q=coldplay&type=CONCERT&city=hanoi&date=2025-12`
- [ ] Full-text search + filter + sort theo date/price
- [ ] Autocomplete suggest: `GET /api/v1/search/suggest?q=cold`

### 3.2 Build `cms-service` (Port 8087)

- [ ] MongoDB: `HomepageConfig`, `Banner`, `FeaturedEvent`
- [ ] CRUD config homepage: banner, featured events, categories (ADMIN only)
- [ ] Public endpoint: `GET /api/v1/cms/homepage` — trả về cấu hình trang chủ

### 3.3 Build `media-service` (Port 8088)

- [ ] Multipart upload → resize 3 kích thước (thumbnail / card / banner) → upload Cloudflare R2
- [ ] Trả về CDN URL: `https://cdn.ticketing.com/events/{eventId}/{size}.webp`
- [ ] Kafka: publish `media.uploaded` → event-catalog-service cập nhật URL ảnh
- [ ] Endpoint: `POST /api/v1/media/upload` (ADMIN only)

---

## Phase 4 — Production Readiness

- [ ] Distributed tracing: Micrometer + Zipkin (add agent mỗi service)
- [ ] Spring Cloud Config Server: tách config ra file riêng theo env
- [ ] Eureka Server: service discovery
- [ ] Docker Compose cập nhật: thêm tất cả service + MongoDB + Elasticsearch
- [ ] CI/CD: GitHub Actions — build + test + Docker image push
- [ ] Môi trường: `dev` / `staging` / `prod` profiles

---

## Thứ tự build tiếp theo

```
user-service (fix) → api-gateway → event-catalog-service
    → booking-service → payment-service → notification-service
        → search-service → cms-service → media-service
            → production readiness
```

**Lý do thứ tự này:**
- `api-gateway` cần sớm để test flow xuyên suốt từ Phase 2
- `event-catalog-service` là dependency của `booking-service`
- `booking-service` + `payment-service` phải đi liền nhau (Kafka coupling chặt)
- `notification-service` có thể build song song với Phase 2 vì chỉ consume Kafka

---

*Cập nhật file này khi hoàn thành một service hoặc thay đổi thứ tự ưu tiên.*
