# event-catalog-service — Design Spec

> Date: 2026-04-13
> Status: Approved
> Port: 8082
> DB: MongoDB Reactive
> Web style: `@RestController` (annotation-based)

---

## 1. Overview

`event-catalog-service` quản lý toàn bộ catalog sự kiện (concert, phim), venue, seat inventory, và location master data. Là service trung tâm cung cấp dữ liệu cho FE và các service khác (booking, search).

**Scope:**
- CRUD Events (Concert + Movie) với lifecycle DRAFT → ON_SALE → SOLD_OUT / CANCELLED / COMPLETED
- CRUD Venues với địa chỉ đầy đủ (province, district, street, lat/lng)
- Auto-generate seats khi event chuyển sang ON_SALE
- Seat status sync từ booking-service qua Kafka
- Location master data (Province + District) read-only, seeded lúc startup
- Kafka producer: `event.published`, `event.updated`
- Kafka consumer skeleton: `media.uploaded`

---

## 2. Project Structure

```
event-catalog-service/
├── src/main/java/com/ticketing/catalog/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Event.java
│   │   │   ├── EventDetail.java          # sealed interface
│   │   │   ├── ConcertDetail.java        # record implements EventDetail
│   │   │   ├── MovieDetail.java          # record implements EventDetail
│   │   │   ├── Venue.java
│   │   │   ├── ZoneConfig.java           # record — zone template trong Venue
│   │   │   ├── EventZone.java            # record — zone override trong Event
│   │   │   ├── Seat.java
│   │   │   └── Location.java
│   │   └── repository/
│   │       ├── EventRepository.java
│   │       ├── VenueRepository.java
│   │       ├── SeatRepository.java
│   │       └── LocationRepository.java
│   ├── application/
│   │   └── service/
│   │       ├── EventService.java
│   │       ├── VenueService.java
│   │       ├── SeatService.java
│   │       └── LocationService.java
│   ├── api/
│   │   ├── controller/
│   │   │   ├── EventController.java
│   │   │   ├── VenueController.java
│   │   │   ├── SeatController.java
│   │   │   └── LocationController.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── CreateEventRequest.java
│   │       │   ├── UpdateEventRequest.java
│   │       │   ├── UpdateEventStatusRequest.java
│   │       │   └── CreateVenueRequest.java
│   │       └── response/
│   │           ├── EventResponse.java
│   │           ├── EventSummaryResponse.java
│   │           ├── VenueResponse.java
│   │           ├── SeatResponse.java
│   │           └── LocationResponse.java
│   ├── infrastructure/
│   │   ├── config/
│   │   │   ├── MongoConfig.java          # indexes
│   │   │   └── LocationDataSeeder.java   # seed provinces + districts lúc startup
│   │   ├── mapper/
│   │   │   ├── EventMapper.java          # MapStruct
│   │   │   └── VenueMapper.java
│   │   └── messaging/
│   │       ├── TicketStatusConsumer.java # consume ticket.reserved/released/issued
│   │       └── MediaUploadedConsumer.java # skeleton — log + TODO
│   └── EventCatalogApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── data/
│       └── locations.json               # 63 tỉnh + quận/huyện seed data
└── pom.xml
```

---

## 3. Domain Model

### `events` collection

```java
@Document("events")
public class Event {
    @Id
    private String id;
    private String title;
    private String description;
    private EventType type;           // CONCERT | MOVIE
    private EventStatus status;       // DRAFT | ON_SALE | SOLD_OUT | CANCELLED | COMPLETED
    private String venueId;
    private String venueName;         // denormalized — tránh lookup
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<EventZone> zones;    // override từ venue zones
    private List<String> imageUrls;   // populated qua media.uploaded Kafka event
    private EventDetail detail;       // ConcertDetail | MovieDetail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;         // userId từ X-User-Id header
}
```

### `EventDetail` sealed interface (Java 25)

```java
public sealed interface EventDetail permits ConcertDetail, MovieDetail {}

public record ConcertDetail(
    List<String> artists,
    List<String> genres,
    Integer ageRestriction
) implements EventDetail {}

public record MovieDetail(
    String director,
    List<String> cast,
    MovieFormat format,       // 2D | 3D | IMAX
    Integer durationMinutes,
    AgeRating rating          // G | PG | PG13 | R
) implements EventDetail {}
```

### `EventZone` record (embedded trong Event)

```java
public record EventZone(
    String name,         // "VIP", "FLOOR", "BALCONY"
    int rows,
    int seatsPerRow,
    BigDecimal price,
    String rowPrefix     // "A", "B"... để gen A1, A2...
)
```

### `venues` collection

```java
@Document("venues")
public class Venue {
    @Id
    private String id;
    private String name;
    private String provinceCode;
    private String provinceName;    // denormalized
    private String districtCode;
    private String districtName;    // denormalized
    private String streetAddress;   // "123 Nguyễn Huệ"
    private Double lat;
    private Double lng;
    private int totalCapacity;
    private List<ZoneConfig> zones; // template — dùng khi tạo event
    private LocalDateTime createdAt;
}

public record ZoneConfig(
    String name,
    int rows,
    int seatsPerRow,
    BigDecimal defaultPrice,
    String rowPrefix
)
```

### `seats` collection

```java
@Document("seats")
public class Seat {
    @Id
    private String id;
    private String eventId;
    private String zone;          // "VIP", "FLOOR"
    private String row;           // "A", "B", "C"
    private int seatNumber;       // 1, 2, 3...
    private String code;          // "A1", "A2"... computed on generate
    private BigDecimal price;
    private SeatStatus status;    // AVAILABLE | LOCKED | SOLD
}
// Indexes:
//   { eventId: 1, zone: 1, status: 1 }
//   { eventId: 1, row: 1, seatNumber: 1 } — unique
```

### `locations` collection

```java
@Document("locations")
public class Location {
    @Id
    private String id;
    private String type;        // "PROVINCE" | "DISTRICT"
    private String code;        // "HN", "HCM", "Q1"...
    private String name;        // "Hà Nội", "Quận 1"
    private String parentCode;  // districtCode → provinceCode (null for province)
}
```

---

## 4. API Endpoints

### Events

| Method | URL | Auth | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/events` | ADMIN | Tạo event (status = DRAFT) |
| `GET` | `/api/v1/events/{id}` | Public | Chi tiết event |
| `GET` | `/api/v1/events` | Public | List + filter: `?type=CONCERT&provinceCode=HN&status=ON_SALE&page=0&size=20` |
| `PUT` | `/api/v1/events/{id}` | ADMIN | Update metadata (chỉ khi DRAFT) |
| `PUT` | `/api/v1/events/{id}/status` | ADMIN | Chuyển status |
| `DELETE` | `/api/v1/events/{id}` | ADMIN | Soft cancel → CANCELLED |

### Venues

| Method | URL | Auth | Mô tả |
|---|---|---|---|
| `POST` | `/api/v1/venues` | ADMIN | Tạo venue |
| `GET` | `/api/v1/venues` | Public | List, filter `?provinceCode=HN` |
| `GET` | `/api/v1/venues/{id}` | Public | Chi tiết venue |
| `PUT` | `/api/v1/venues/{id}` | ADMIN | Update venue |

### Seats

| Method | URL | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/events/{id}/seats` | Public | Tất cả seats, group by zone+row |
| `GET` | `/api/v1/events/{id}/seats?zone=VIP` | Public | Filter theo zone |

### Locations

| Method | URL | Auth | Mô tả |
|---|---|---|---|
| `GET` | `/api/v1/locations/provinces` | Public | 63 tỉnh/thành (cached 24h) |
| `GET` | `/api/v1/locations/provinces/{code}/districts` | Public | Quận/huyện theo tỉnh |

---

## 5. Status Transition Rules

```
DRAFT      ──► ON_SALE    → generate seats + publish "event.published"
DRAFT      ──► CANCELLED  → không generate seats
ON_SALE    ──► SOLD_OUT   → booking-service publish "event.soldout" → catalog consume + update status
ON_SALE    ──► CANCELLED  → xóa seats + notify
ON_SALE    ──► COMPLETED  → sau startTime
SOLD_OUT   ──► COMPLETED  → sau startTime
```

**Rule:** Chỉ cho phép update metadata (`PUT /events/{id}`) khi status = `DRAFT`. Sau khi ON_SALE, mọi thay đổi metadata phải qua CANCELLED → tạo event mới.

---

## 6. Kafka Integration

### Producer

| Topic | Trigger | Payload |
|---|---|---|
| `event.published` | DRAFT → ON_SALE | `{ eventId, title, type, venueId, startTime, status }` |
| `event.updated` | PUT /events/{id} thành công | `{ eventId, updatedFields[] }` |

### Consumer

| Topic | Handler | Logic |
|---|---|---|
| `ticket.reserved` | `TicketStatusConsumer` | Update seat status → LOCKED |
| `ticket.released` | `TicketStatusConsumer` | Update seat status → AVAILABLE |
| `ticket.issued` | `TicketStatusConsumer` | Update seat status → SOLD |
| `media.uploaded` | `MediaUploadedConsumer` | **SKELETON** — log payload, TODO: update `event.imageUrls` |

---

## 7. Data Flows

### Flow: DRAFT → ON_SALE + Seat Generation

```
PUT /api/v1/events/{id}/status { "status": "ON_SALE" }
  → validate transition hợp lệ
  → seatService.generateSeats(event.zones)
      → for each zone: generate rows × seatsPerRow documents
      → code = rowPrefix + seatNumber (e.g., "A1")
      → seatRepository.saveAll(seats)   # bulk insert
  → eventRepository.save(event)         # status = ON_SALE
  → eventPublisher.publish("event.published", eventId, DomainEvent)
```

### Flow: Seat Status Sync từ booking-service

```
booking-service publishes "ticket.reserved" { seatId, eventId }
  → TicketStatusConsumer.onTicketReserved()
  → seatRepository.updateStatus(seatId, LOCKED)

booking-service publishes "ticket.released" { seatId, eventId }
  → seatRepository.updateStatus(seatId, AVAILABLE)

booking-service publishes "ticket.issued" { seatId, eventId }
  → seatRepository.updateStatus(seatId, SOLD)
```

### Flow: Cache Strategy

```java
// Event detail — cache 30 phút
cacheStrategy.getOrLoad("cache:event:" + id, Duration.ofMinutes(30),
    () -> eventRepository.findById(id).map(mapper::toDto))

// Evict on update/status change
cacheStrategy.evict("cache:event:" + id)

// Locations — cache 24 giờ
cacheStrategy.getOrLoad("cache:locations:provinces", Duration.ofHours(24),
    () -> locationRepository.findByType("PROVINCE").collectList())
```

---

## 8. Error Handling

### Service-specific exceptions

```java
// extend từ common-base exceptions
EventNotFoundException    extends ResourceNotFoundException   // 404
VenueNotFoundException    extends ResourceNotFoundException   // 404
InvalidStatusTransitionException extends ConflictException   // 409
```

### Key error scenarios

| Scenario | Exception | HTTP |
|---|---|---|
| Event/Venue không tồn tại | `EventNotFoundException` | 404 |
| Status transition không hợp lệ | `InvalidStatusTransitionException` | 409 |
| Update event không phải DRAFT | `ConflictException` | 409 |
| VenueId không tồn tại khi tạo event | `VenueNotFoundException` | 404 |

---

## 9. Testing Plan

### Service layer (StepVerifier)

```java
// EventServiceTest
void shouldCreateEventWithDraftStatus()
void shouldGenerateSeatsWhenTransitionToOnSale()
void shouldThrowWhenTransitionInvalid()        // ON_SALE → DRAFT
void shouldThrowWhenVenueNotFoundOnCreate()
void shouldEvictCacheOnUpdate()

// SeatServiceTest
void shouldGenerateCorrectSeatCount()          // zones × rows × seatsPerRow
void shouldGenerateCorrectSeatCodes()          // A1, A2, B1...
void shouldUpdateSeatStatusOnKafkaEvent()
```

### Controller layer (WebTestClient)

```java
// EventControllerTest
void shouldReturn201WhenEventCreated()
void shouldReturn404WhenEventNotFound()
void shouldReturn409WhenInvalidStatusTransition()
void shouldReturn403WhenNotAdmin()
void shouldReturn200WithFilteredEvents()       // ?type=CONCERT&provinceCode=HN
```

---

## 10. Dependencies (pom.xml)

```xml
spring-boot-starter-webflux
spring-boot-starter-data-mongodb-reactive
spring-boot-starter-data-redis-reactive
spring-boot-starter-validation
spring-boot-starter-actuator
spring-kafka
common-entity (com.ticketing)
common-base (com.ticketing)
lombok
mapstruct
springdoc-openapi-starter-webflux-ui
```

---

## 11. application.yml — Key Config

```yaml
server:
  port: 8082

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/ticketing_catalog
  redis:
    host: localhost
    port: 6379
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: event-catalog-service

common:
  openapi:
    title: "Event Catalog Service API"
    description: "Events, venues, seats, locations"
```
