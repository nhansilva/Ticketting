# Ticketing System - Concert Ticket Booking

Hệ thống đặt vé concert với kiến trúc microservices, được thiết kế để xử lý flash sales và chống double-booking.

## 🏗️ Kiến trúc

### Tech Stack
- **Backend**: Java 25 (LTS), Spring Boot 4.0.1, Spring WebFlux (Reactive)
- **Database**: PostgreSQL (ACID transactions)
- **Cache & Locks**: Redis (Distributed locks, caching)
- **Message Queue**: Kafka (Event streaming)
- **Build Tool**: Maven (Multi-module)

### Microservices
1. **API Gateway** - Entry point, routing, rate limiting
2. **User Service** - User management, authentication
3. **Event Service** - Event/Concert management
4. **Inventory Service** - Ticket inventory, anti-double-booking
5. **Booking Service** - Booking orchestration
6. **Payment Service** - Payment processing
7. **Notification Service** - Email/SMS notifications

## 📁 Cấu trúc Project

```
ticketing-system/
├── common/                    # Shared modules
│   ├── common-dto/           # DTOs, Enums, Constants
│   ├── common-exception/     # Custom exceptions, Error handling
│   ├── common-config/        # Shared configurations
│   └── common-utils/         # Utility classes (Redis locks, cache, rate limiting)
├── api-gateway/              # API Gateway service
├── user-service/             # User service
├── event-service/            # Event service
├── inventory-service/        # Inventory service (Critical)
├── booking-service/          # Booking service
├── payment-service/          # Payment service
└── notification-service/     # Notification service
```

## 🚀 Quick Start

### Prerequisites
- Java 25 (LTS)
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15+
- Redis 7+
- Kafka (optional)

### Local Development

1. **Start infrastructure services:**
```bash
docker-compose up -d
```

2. **Build project:**
```bash
mvn clean install
```

3. **Run services:**
```bash
# Run individual service
cd inventory-service
mvn spring-boot:run
```

## 🔒 Concurrency Control

### Anti-Double-Booking Mechanisms

1. **Database-Level Locking (PostgreSQL)**
   - `SELECT FOR UPDATE SKIP LOCKED` - Pessimistic locking
   - Row-level locks prevent concurrent updates

2. **Distributed Locks (Redis)**
   - Redis-based locks for cross-service coordination
   - Auto-release with TTL

3. **Optimistic Locking**
   - Version field in ticket table
   - Fail if version changed

### Reservation Pattern
```
1. Check availability (Redis cache)
2. Acquire distributed lock (Redis)
3. Reserve ticket (PostgreSQL with SELECT FOR UPDATE)
4. Create booking (PENDING status)
5. Process payment (async)
6. Confirm or release ticket
```

## 📊 Database Schema

### Inventory DB
- `tickets` - Ticket inventory with status and version
- Indexes on `(event_id, status)` for performance

### Bookings DB
- `bookings` - Booking records
- `booking_tickets` - Many-to-many relationship

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific module tests
cd inventory-service
mvn test
```

## 📝 Notes

- All services use **Spring WebFlux** (reactive, non-blocking)
- **R2DBC** for reactive database access
- **Redis** for caching and distributed locks
- **Kafka** for async event processing

## 🔄 Next Steps

1. Implement Inventory Service with locking mechanisms
2. Implement Booking Service with Saga pattern
3. Add API Gateway with rate limiting
4. Setup monitoring and logging
5. Load testing for flash sales

