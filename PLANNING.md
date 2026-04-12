# PLANNING.md — Event Ticketing Platform

> Cập nhật: 2026-04-12
> Trạng thái tổng thể: **Phase 1 gần hoàn thành — user-service ✅, api-gateway ✅, ticketing-client 🟡, Google OAuth2 🟡 (cần điền credentials)**

---

## Tình trạng hiện tại

| Service / Module | Trạng thái | Ghi chú |
|---|---|---|
| `common-entity` | 🟢 Done | DTOs, enums, constants, interfaces |
| `common-base` | 🟢 Done | Config, exception, cache, lock, ratelimit, messaging |
| `user-service` | 🟢 Done | Auth, profile, JWT, Google OAuth2 code xong |
| `api-gateway` | 🟢 Done | JWT filter, rate limit, correlation ID |
| `ticketing-client` | 🟡 In Progress | Auth pages ✅, Home ✅, Google OAuth2 callback ✅ — cần test e2e |
| `event-catalog-service` | 🔴 Chưa bắt đầu | Next priority |
| `booking-service` | 🔴 Chưa bắt đầu | — |
| `payment-service` | 🔴 Chưa bắt đầu | — |
| `notification-service` | 🔴 Chưa bắt đầu | — |
| `search-service` | 🔴 Chưa bắt đầu | — |
| `cms-service` | 🔴 Chưa bắt đầu | — |
| `media-service` | 🔴 Chưa bắt đầu | — |

---

## Phase 1 — Core Foundation

### 1.1 `common` library ✅ Hoàn thành 2026-04-11

- [x] Restructure 4 module → 2 module: `common-entity` + `common-base`
- [x] Design patterns: Strategy, Observer, Builder, Template Method, Adapter
- [x] `META-INF/spring/AutoConfiguration.imports` — auto-load tất cả configs
- [x] `ApiResponse<T>`, `PageResponse<T>`, `PageRequest`
- [x] `CacheStrategy`, `DistributedLock`, `RateLimiter` (Redis impl)
- [x] `EventPublisher`, `EventConsumer`, `DomainEvent`
- [x] `GlobalExceptionHandler`, `TicketingException` hierarchy
- [x] `KafkaConfig`, `RedisConfig`, `RabbitMQConfig`, `WebFluxConfig`

---

### 1.2 `user-service` ✅ Hoàn thành 2026-04-12

**Auth & Profile:**
- [x] Register, Login, RefreshToken, VerifyEmail
- [x] ForgotPassword, ResetPassword, ChangePassword
- [x] Profile CRUD, Preferences CRUD
- [x] Soft delete, Deactivate, Reactivate account
- [x] JWT generation + validation (JJWT 0.12.5)
- [x] BCrypt password encoding
- [x] `AdminDataSeeder` — seed admin user lúc startup
- [x] `JwtAuthenticationFilter` — WebFilter extract + inject auth context
- [x] `SecurityConfig` — CORS, public/protected routes, ADMIN role

**Fixes đã xử lý:**
- [x] R2DBC `Persistable<UUID>` — fix INSERT/UPDATE confusion
- [x] Phone validation regex cho số VN: `^(\+?84|0)[3-9]\d{8}$`
- [x] Login cho phép `PENDING_VERIFICATION` status
- [x] CORS: `setAllowedOriginPatterns` thay vì wildcard origins
- [x] Schema migration → `schema.sql` + `spring.sql.init`

**Google OAuth2 (code xong, chờ credentials):**
- [x] `GoogleOAuth2UserService` — upsert user từ Google profile
- [x] `OAuth2SuccessHandler` — JWT → redirect FE `/auth/callback`
- [x] `OAuth2FailureHandler` — redirect FE với error
- [x] `SecurityConfig` — `.oauth2Login()` config
- [x] `User` entity — thêm `googleId`, `authProvider (LOCAL|GOOGLE)`
- [x] `schema.sql` — thêm `google_id`, `auth_provider`, `password_hash` nullable
- [ ] **⚠️ Việc còn lại:** Điền `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` vào `application.yml`
- [ ] **⚠️ Việc còn lại:** Drop & recreate bảng `users` để có column mới

---

### 1.3 `api-gateway` ✅ Hoàn thành 2026-04-11

- [x] `JwtAuthFilter` — validate JWT, inject `X-User-Id` + `X-User-Role`
- [x] `RateLimitFilter` — Sliding Window Log, PER_USER/PER_IP mode
- [x] `CorrelationIdFilter` — inject `X-Correlation-Id` + `X-Request-Id`
- [x] `GatewayErrorWebExceptionHandler` — format chuẩn error response
- [x] Routes cho 7 services + Swagger aggregation `/v3/api-docs/{service}`
- [x] Swagger UI aggregate tất cả services

---

### 1.4 `ticketing-client` 🟡 In Progress

**Đã hoàn thành:**
- [x] Setup React + Vite + TypeScript
- [x] Dependencies: react-router-dom, axios, react-hook-form, zod, zustand, @tanstack/react-query
- [x] `axiosInstance` — interceptor tự động attach JWT + refresh token
- [x] `useAuthStore` (Zustand + persist localStorage)
- [x] `PrivateRoute` + `PublicRoute`
- [x] `LoginPage` — form + validation + nút Google OAuth2
- [x] `RegisterPage` — form + success state
- [x] `ForgotPasswordPage` — gửi link reset
- [x] `ResetPasswordPage` — đặt mật khẩu mới từ token URL
- [x] `CallbackPage` — nhận token OAuth2 từ URL → lưu store → redirect
- [x] `HomePage` — navbar, search, hero, tab Concert/Phim, grid cards mock data
- [x] Dark theme glassmorphism UI (CSS Modules)

**Việc còn lại:**
- [ ] Test Google OAuth2 end-to-end sau khi BE có credentials
- [ ] Kết nối API thật khi `event-catalog-service` xong (thay mock data)
- [ ] Trang chi tiết event
- [ ] Trang booking flow
- [ ] Trang "Vé của tôi"

---

### 1.5 Infrastructure ✅ Hoàn thành 2026-04-11

- [x] `docker-compose.yml` — đầy đủ: Postgres, Redis, MongoDB, Kafka, Zookeeper, Elasticsearch, RabbitMQ, Zipkin
- [x] UIs: Redis Insight `:5540`, Mongo Express `:8091`, Kafka UI `:9000`
- [x] Init scripts: `scripts/postgres/init-multiple-dbs.sh`, `scripts/mongodb/init.js`
- [x] Fix Mongo Express port `8081` → `8091` (conflict với user-service)

---

## Phase 2 — Core Business

### 2.1 `event-catalog-service` (Port 8082) 🔴 Next

MongoDB Reactive + `@RestController`.

- [ ] Domain: `Event` (sealed interface `ConcertDetail | MovieDetail`), `Venue`
- [ ] CRUD Events: `POST`, `GET /{id}`, `GET ?type&city&page`, `PUT /{id}`, `PUT /{id}/status`
- [ ] CRUD Venues: `GET`, `POST`
- [ ] Seat management: `GET /events/{id}/seats`, auto-generate seats theo venue zones
- [ ] Kafka producer: `event.published`, `event.updated`
- [ ] Test: CRUD event, filter theo type/city, seat generation

---

### 2.2 `booking-service` (Port 8083) 🔴

Functional routing — service phức tạp nhất.

- [ ] Domain: `Booking`, `BookingItem`, `Ticket`
- [ ] Redis seat locking TTL 10 phút: `SETNX booking:seat:{seatId}`
- [ ] `POST /api/v1/bookings` — lock seats → `PENDING`
- [ ] `POST /api/v1/bookings/{id}/cancel` — release lock → `CANCELLED`
- [ ] `GET /api/v1/bookings/{id}`, `GET /api/v1/bookings/my`
- [ ] Business rules: max 4 vé/user/event, priceSnapshot
- [ ] Kafka: consume `payment.completed/failed`, publish `booking.created/expired`, `ticket.issued`

---

### 2.3 `payment-service` (Port 8084) 🔴

Functional routing — webhook cần verify signature.

- [ ] Domain: `Payment`
- [ ] `POST /api/v1/payments/initiate` → Payment URL
- [ ] Webhook: `/webhook/vnpay`, `/webhook/momo` — verify HMAC
- [ ] `POST /api/v1/payments/{id}/refund` (ADMIN)
- [ ] Payment timeout 15 phút
- [ ] Kafka: publish `payment.completed`, `payment.failed`

---

### 2.4 `notification-service` (Port 8085) 🔴

Chỉ consume Kafka, không có HTTP controller.

- [ ] Kafka consumers: `booking.created/expired`, `payment.completed/failed`, `ticket.issued`
- [ ] Email templates HTML (Thymeleaf/Freemarker)
- [ ] SMTP: Mailgun hoặc SendGrid

---

## Phase 3 — Enhancement

### 3.1 `search-service` (Port 8086)
- [ ] Elasticsearch index `events`
- [ ] Consume `event.published/updated` → upsert ES
- [ ] Full-text search + filter + autocomplete suggest

### 3.2 `cms-service` (Port 8087)
- [ ] MongoDB: `HomepageConfig`, `Banner`, `FeaturedEvent`
- [ ] ADMIN CRUD + public `GET /api/v1/cms/homepage`

### 3.3 `media-service` (Port 8088)
- [ ] Multipart upload → resize → Cloudflare R2
- [ ] CDN URL: `https://cdn.ticketing.com/events/{eventId}/{size}.webp`
- [ ] Kafka: `media.uploaded`

---

## Phase 4 — Production Readiness

- [ ] Distributed tracing: Micrometer + Zipkin
- [ ] Spring Cloud Config Server
- [ ] Eureka Server — service discovery
- [ ] CI/CD: GitHub Actions — build + test + Docker push
- [ ] `dev` / `staging` / `prod` profiles

---

## ⚠️ Việc cần làm ngay (blockers)

| # | Việc | Lý do |
|---|------|-------|
| 1 | Điền `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` vào `user-service/application.yml` | Google OAuth2 chưa hoạt động |
| 2 | Recreate bảng `users` (thêm `google_id`, `auth_provider`) | Schema cũ chưa có columns mới |
| 3 | Test Google OAuth2 end-to-end | Verify flow hoàn chỉnh |
| 4 | Build `event-catalog-service` | FE đang dùng mock data, cần API thật |

---

## Thứ tự build tiếp theo

```
⚠️ Finish Google OAuth2 (credentials + test)
  → event-catalog-service (replace mock data FE)
    → booking-service + payment-service (song song)
      → notification-service
        → search-service → cms-service → media-service
          → production readiness
```

---

*Cập nhật: 2026-04-12 — Cập nhật trạng thái sau session làm việc ngày 12/04.*
