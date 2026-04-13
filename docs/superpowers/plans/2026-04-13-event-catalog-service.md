# Event Catalog Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `event-catalog-service` (port 8082) — quản lý events (concert/movie), venues, seats, và location master data với MongoDB Reactive + Kafka.

**Architecture:** `@RestController` annotation-based WebFlux. Flat `seats` collection với compound index, seats auto-generated khi event chuyển sang `ON_SALE`. Seat status sync từ booking-service qua Kafka consumer.

**Tech Stack:** Spring Boot 3.4.4, Spring WebFlux, Spring Data MongoDB Reactive, Spring Data Redis Reactive, Spring Kafka, MapStruct 1.6.2, Lombok, springdoc-openapi-starter-webflux-ui 2.8.3, Java 25.

---

## File Map

| File | Trách nhiệm |
|---|---|
| `event-catalog-service/pom.xml` | Maven dependencies |
| `EventCatalogApplication.java` | Main class |
| `application.yml` | Config: MongoDB, Redis, Kafka, port 8082 |
| `domain/model/Event.java` | @Document("events") — entity chính |
| `domain/model/EventDetail.java` | sealed interface ConcertDetail/MovieDetail |
| `domain/model/ConcertDetail.java` | record — artists, genres, ageRestriction |
| `domain/model/MovieDetail.java` | record — director, cast, format, duration, rating |
| `domain/model/EventZone.java` | record — zone override per-event |
| `domain/model/Venue.java` | @Document("venues") |
| `domain/model/ZoneConfig.java` | record — zone template trong Venue |
| `domain/model/Seat.java` | @Document("seats") |
| `domain/model/Location.java` | @Document("locations") — province/district |
| `domain/model/enums/*.java` | EventType, SeatStatus, MovieFormat, AgeRating |
| `domain/repository/EventRepository.java` | ReactiveMongoRepository + custom queries |
| `domain/repository/VenueRepository.java` | ReactiveMongoRepository |
| `domain/repository/SeatRepository.java` | ReactiveMongoRepository + updateStatus |
| `domain/repository/LocationRepository.java` | ReactiveMongoRepository |
| `application/service/EventService.java` | CRUD + status transition + Kafka publish |
| `application/service/VenueService.java` | CRUD venues |
| `application/service/SeatService.java` | generate seats + query grouped |
| `application/service/LocationService.java` | read provinces/districts with cache |
| `api/controller/EventController.java` | @RestController /api/v1/events |
| `api/controller/VenueController.java` | @RestController /api/v1/venues |
| `api/controller/SeatController.java` | @RestController /api/v1/events/{id}/seats |
| `api/controller/LocationController.java` | @RestController /api/v1/locations |
| `api/dto/request/*.java` | CreateEventRequest, UpdateEventRequest, etc. |
| `api/dto/response/*.java` | EventResponse, EventSummaryResponse, etc. |
| `infrastructure/config/MongoConfig.java` | Indexes creation |
| `infrastructure/config/SecurityConfig.java` | Public routes + ADMIN header check |
| `infrastructure/config/LocationDataSeeder.java` | Seed locations.json → MongoDB |
| `infrastructure/mapper/EventMapper.java` | MapStruct: Event ↔ DTOs |
| `infrastructure/mapper/VenueMapper.java` | MapStruct: Venue ↔ DTOs |
| `infrastructure/messaging/TicketStatusConsumer.java` | @KafkaListener: reserved/released/issued |
| `infrastructure/messaging/MediaUploadedConsumer.java` | Skeleton: log + TODO |
| `common/exception/EventNotFoundException.java` | extends ResourceNotFoundException |
| `common/exception/VenueNotFoundException.java` | extends ResourceNotFoundException |
| `common/exception/InvalidStatusTransitionException.java` | extends ConflictException |
| `src/main/resources/data/locations.json` | 63 tỉnh + quận/huyện seed data |
| `src/test/.../service/EventServiceTest.java` | StepVerifier tests |
| `src/test/.../service/SeatServiceTest.java` | StepVerifier tests |
| `src/test/.../controller/EventControllerTest.java` | WebTestClient tests |

---

## Task 1: Project Scaffold

**Files:**
- Create: `event-catalog-service/pom.xml`
- Create: `event-catalog-service/src/main/java/com/ticketing/catalog/EventCatalogApplication.java`
- Create: `event-catalog-service/src/main/resources/application.yml`
- Modify: `pom.xml` (root) — thêm module

- [ ] **Step 1: Tạo pom.xml cho event-catalog-service**

```xml
<!-- event-catalog-service/pom.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.ticketing</groupId>
        <artifactId>ticketing-system</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>event-catalog-service</artifactId>
    <packaging>jar</packaging>
    <name>Event Catalog Service</name>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
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
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>io.projectreactor</groupId>
            <artifactId>reactor-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Tạo EventCatalogApplication.java**

```java
// event-catalog-service/src/main/java/com/ticketing/catalog/EventCatalogApplication.java
package com.ticketing.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EventCatalogApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventCatalogApplication.class, args);
    }
}
```

- [ ] **Step 3: Tạo application.yml**

```yaml
# event-catalog-service/src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: event-catalog-service
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/ticketing_catalog}
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: event-catalog-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.ticketing.*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

management:
  endpoints:
    web:
      exposure:
        include: health,info

common:
  openapi:
    title: "Event Catalog Service API"
    description: "Events, venues, seats, locations"

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 4: Thêm module vào root pom.xml**

Trong file `pom.xml` (root), tìm `<modules>` block và thêm:
```xml
<module>event-catalog-service</module>
```

- [ ] **Step 5: Verify build scaffold**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/pom.xml event-catalog-service/src/main/java/com/ticketing/catalog/EventCatalogApplication.java event-catalog-service/src/main/resources/application.yml pom.xml
git commit -m "feat(event-catalog): scaffold project structure"
```

---

## Task 2: Domain Models & Enums

**Files:**
- Create: `domain/model/enums/EventType.java`
- Create: `domain/model/enums/SeatStatus.java`
- Create: `domain/model/enums/MovieFormat.java`
- Create: `domain/model/enums/AgeRating.java`
- Create: `domain/model/EventDetail.java`
- Create: `domain/model/ConcertDetail.java`
- Create: `domain/model/MovieDetail.java`
- Create: `domain/model/EventZone.java`
- Create: `domain/model/ZoneConfig.java`
- Create: `domain/model/Event.java`
- Create: `domain/model/Venue.java`
- Create: `domain/model/Seat.java`
- Create: `domain/model/Location.java`

Base package cho tất cả files: `event-catalog-service/src/main/java/com/ticketing/catalog/`

- [ ] **Step 1: Tạo enums**

```java
// domain/model/enums/EventType.java
package com.ticketing.catalog.domain.model.enums;
public enum EventType { CONCERT, MOVIE }
```

```java
// domain/model/enums/SeatStatus.java
package com.ticketing.catalog.domain.model.enums;
public enum SeatStatus { AVAILABLE, LOCKED, SOLD }
```

```java
// domain/model/enums/MovieFormat.java
package com.ticketing.catalog.domain.model.enums;
public enum MovieFormat { TWO_D, THREE_D, IMAX }
```

```java
// domain/model/enums/AgeRating.java
package com.ticketing.catalog.domain.model.enums;
public enum AgeRating { G, PG, PG13, R }
```

- [ ] **Step 2: Tạo EventDetail sealed interface + ConcertDetail + MovieDetail**

```java
// domain/model/EventDetail.java
package com.ticketing.catalog.domain.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ConcertDetail.class, name = "CONCERT"),
    @JsonSubTypes.Type(value = MovieDetail.class, name = "MOVIE")
})
public sealed interface EventDetail permits ConcertDetail, MovieDetail {}
```

```java
// domain/model/ConcertDetail.java
package com.ticketing.catalog.domain.model;

import java.util.List;

public record ConcertDetail(
    List<String> artists,
    List<String> genres,
    Integer ageRestriction
) implements EventDetail {}
```

```java
// domain/model/MovieDetail.java
package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.AgeRating;
import com.ticketing.catalog.domain.model.enums.MovieFormat;
import java.util.List;

public record MovieDetail(
    String director,
    List<String> cast,
    MovieFormat format,
    Integer durationMinutes,
    AgeRating rating
) implements EventDetail {}
```

- [ ] **Step 3: Tạo EventZone + ZoneConfig records**

```java
// domain/model/EventZone.java
package com.ticketing.catalog.domain.model;

import java.math.BigDecimal;

public record EventZone(
    String name,
    int rows,
    int seatsPerRow,
    BigDecimal price,
    String rowPrefix
) {}
```

```java
// domain/model/ZoneConfig.java
package com.ticketing.catalog.domain.model;

import java.math.BigDecimal;

public record ZoneConfig(
    String name,
    int rows,
    int seatsPerRow,
    BigDecimal defaultPrice,
    String rowPrefix
) {}
```

- [ ] **Step 4: Tạo Event entity**

```java
// domain/model/Event.java
package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("events")
public class Event {
    @Id
    private String id;
    private String title;
    private String description;
    private EventType type;
    private EventStatus status;
    private String venueId;
    private String venueName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<EventZone> zones;
    private List<String> imageUrls;
    private EventDetail detail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
```

- [ ] **Step 5: Tạo Venue entity**

```java
// domain/model/Venue.java
package com.ticketing.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("venues")
public class Venue {
    @Id
    private String id;
    private String name;
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String streetAddress;
    private Double lat;
    private Double lng;
    private int totalCapacity;
    private List<ZoneConfig> zones;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 6: Tạo Seat entity**

```java
// domain/model/Seat.java
package com.ticketing.catalog.domain.model;

import com.ticketing.catalog.domain.model.enums.SeatStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("seats")
public class Seat {
    @Id
    private String id;
    private String eventId;
    private String zone;
    private String row;
    private int seatNumber;
    private String code;
    private BigDecimal price;
    private SeatStatus status;
}
```

- [ ] **Step 7: Tạo Location entity**

```java
// domain/model/Location.java
package com.ticketing.catalog.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("locations")
public class Location {
    @Id
    private String id;
    private String type;        // "PROVINCE" | "DISTRICT"
    private String code;
    private String name;
    private String parentCode;  // null for PROVINCE
}
```

- [ ] **Step 8: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/domain/
git commit -m "feat(event-catalog): add domain models and enums"
```

---

## Task 3: Exceptions + DTOs

**Files:**
- Create: `common/exception/EventNotFoundException.java`
- Create: `common/exception/VenueNotFoundException.java`
- Create: `common/exception/InvalidStatusTransitionException.java`
- Create: `api/dto/request/CreateEventRequest.java`
- Create: `api/dto/request/UpdateEventRequest.java`
- Create: `api/dto/request/UpdateEventStatusRequest.java`
- Create: `api/dto/request/CreateVenueRequest.java`
- Create: `api/dto/response/EventResponse.java`
- Create: `api/dto/response/EventSummaryResponse.java`
- Create: `api/dto/response/VenueResponse.java`
- Create: `api/dto/response/SeatResponse.java`
- Create: `api/dto/response/LocationResponse.java`

- [ ] **Step 1: Tạo exceptions**

```java
// common/exception/EventNotFoundException.java
package com.ticketing.catalog.common.exception;

import com.ticketing.common.exception.domain.ResourceNotFoundException;

public class EventNotFoundException extends ResourceNotFoundException {
    public EventNotFoundException(String id) {
        super("Event", id);
    }
}
```

```java
// common/exception/VenueNotFoundException.java
package com.ticketing.catalog.common.exception;

import com.ticketing.common.exception.domain.ResourceNotFoundException;

public class VenueNotFoundException extends ResourceNotFoundException {
    public VenueNotFoundException(String id) {
        super("Venue", id);
    }
}
```

```java
// common/exception/InvalidStatusTransitionException.java
package com.ticketing.catalog.common.exception;

import com.ticketing.common.exception.domain.ConflictException;
import com.ticketing.common.dto.enums.EventStatus;

public class InvalidStatusTransitionException extends ConflictException {
    public InvalidStatusTransitionException(EventStatus from, EventStatus to) {
        super("Invalid status transition: " + from + " → " + to);
    }
}
```

- [ ] **Step 2: Tạo request DTOs**

```java
// api/dto/request/CreateEventRequest.java
package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull EventType type,
    @NotBlank String venueId,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotNull @Size(min = 1) List<EventZone> zones,
    @NotNull EventDetail detail
) {}
```

```java
// api/dto/request/UpdateEventRequest.java
package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record UpdateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull LocalDateTime startTime,
    @NotNull LocalDateTime endTime,
    @NotNull List<EventZone> zones,
    @NotNull EventDetail detail
) {}
```

```java
// api/dto/request/UpdateEventStatusRequest.java
package com.ticketing.catalog.api.dto.request;

import com.ticketing.common.dto.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateEventStatusRequest(@NotNull EventStatus status) {}
```

```java
// api/dto/request/CreateVenueRequest.java
package com.ticketing.catalog.api.dto.request;

import com.ticketing.catalog.domain.model.ZoneConfig;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateVenueRequest(
    @NotBlank String name,
    @NotBlank String provinceCode,
    @NotBlank String provinceName,
    @NotBlank String districtCode,
    @NotBlank String districtName,
    @NotBlank String streetAddress,
    Double lat,
    Double lng,
    @NotNull @Size(min = 1) List<ZoneConfig> zones
) {}
```

- [ ] **Step 3: Tạo response DTOs**

```java
// api/dto/response/EventResponse.java
package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.EventDetail;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
    String id,
    String title,
    String description,
    EventType type,
    EventStatus status,
    String venueId,
    String venueName,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<EventZone> zones,
    List<String> imageUrls,
    EventDetail detail,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

```java
// api/dto/response/EventSummaryResponse.java
package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventSummaryResponse(
    String id,
    String title,
    EventType type,
    EventStatus status,
    String venueName,
    LocalDateTime startTime,
    List<String> imageUrls
) {}
```

```java
// api/dto/response/VenueResponse.java
package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.ZoneConfig;

import java.time.LocalDateTime;
import java.util.List;

public record VenueResponse(
    String id,
    String name,
    String provinceCode,
    String provinceName,
    String districtCode,
    String districtName,
    String streetAddress,
    Double lat,
    Double lng,
    int totalCapacity,
    List<ZoneConfig> zones,
    LocalDateTime createdAt
) {}
```

```java
// api/dto/response/SeatResponse.java
package com.ticketing.catalog.api.dto.response;

import com.ticketing.catalog.domain.model.enums.SeatStatus;

import java.math.BigDecimal;

public record SeatResponse(
    String id,
    String zone,
    String row,
    int seatNumber,
    String code,
    BigDecimal price,
    SeatStatus status
) {}
```

```java
// api/dto/response/LocationResponse.java
package com.ticketing.catalog.api.dto.response;

public record LocationResponse(
    String code,
    String name,
    String parentCode
) {}
```

- [ ] **Step 4: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/common/ event-catalog-service/src/main/java/com/ticketing/catalog/api/dto/
git commit -m "feat(event-catalog): add exceptions and DTOs"
```

---

## Task 4: Repositories

**Files:**
- Create: `domain/repository/EventRepository.java`
- Create: `domain/repository/VenueRepository.java`
- Create: `domain/repository/SeatRepository.java`
- Create: `domain/repository/LocationRepository.java`

- [ ] **Step 1: Tạo EventRepository**

```java
// domain/repository/EventRepository.java
package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface EventRepository extends ReactiveMongoRepository<Event, String> {

    Flux<Event> findByTypeAndStatus(EventType type, EventStatus status, Pageable pageable);

    Flux<Event> findByStatus(EventStatus status, Pageable pageable);

    Flux<Event> findByType(EventType type, Pageable pageable);

    Flux<Event> findAllBy(Pageable pageable);
}
```

- [ ] **Step 2: Tạo VenueRepository**

```java
// domain/repository/VenueRepository.java
package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Venue;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface VenueRepository extends ReactiveMongoRepository<Venue, String> {
    Flux<Venue> findByProvinceCode(String provinceCode);
}
```

- [ ] **Step 3: Tạo SeatRepository**

```java
// domain/repository/SeatRepository.java
package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SeatRepository extends ReactiveMongoRepository<Seat, String> {

    Flux<Seat> findByEventId(String eventId);

    Flux<Seat> findByEventIdAndZone(String eventId, String zone);

    Mono<Void> deleteByEventId(String eventId);

    Mono<Long> countByEventIdAndStatus(String eventId, SeatStatus status);
}
```

- [ ] **Step 4: Tạo LocationRepository**

```java
// domain/repository/LocationRepository.java
package com.ticketing.catalog.domain.repository;

import com.ticketing.catalog.domain.model.Location;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LocationRepository extends ReactiveMongoRepository<Location, String> {
    Flux<Location> findByType(String type);
    Flux<Location> findByParentCode(String parentCode);
    Mono<Boolean> existsByCode(String code);
}
```

- [ ] **Step 5: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/domain/repository/
git commit -m "feat(event-catalog): add repositories"
```

---

## Task 5: Mappers

**Files:**
- Create: `infrastructure/mapper/EventMapper.java`
- Create: `infrastructure/mapper/VenueMapper.java`

- [ ] **Step 1: Tạo EventMapper**

```java
// infrastructure/mapper/EventMapper.java
package com.ticketing.catalog.infrastructure.mapper;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.domain.model.Event;
import com.ticketing.common.dto.enums.EventStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.ticketing.common.dto.enums.EventStatus.DRAFT)")
    @Mapping(target = "imageUrls", expression = "java(new java.util.ArrayList<>())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "venueName", ignore = true)
    Event toEntity(CreateEventRequest request);

    EventResponse toResponse(Event event);

    EventSummaryResponse toSummaryResponse(Event event);
}
```

- [ ] **Step 2: Tạo VenueMapper**

```java
// infrastructure/mapper/VenueMapper.java
package com.ticketing.catalog.infrastructure.mapper;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.domain.model.Venue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalCapacity", expression = "java(request.zones().stream().mapToInt(z -> z.rows() * z.seatsPerRow()).sum())")
    Venue toEntity(CreateVenueRequest request);

    VenueResponse toResponse(Venue venue);
}
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/infrastructure/mapper/
git commit -m "feat(event-catalog): add MapStruct mappers"
```

---

## Task 6: MongoDB Config, Security Config & Location Seeder

**Files:**
- Create: `infrastructure/config/MongoConfig.java`
- Create: `infrastructure/config/SecurityConfig.java`
- Create: `infrastructure/config/LocationDataSeeder.java`
- Create: `src/main/resources/data/locations.json`

- [ ] **Step 1: Tạo MongoConfig — tạo indexes**

```java
// infrastructure/config/MongoConfig.java
package com.ticketing.catalog.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.Index;
import org.bson.Document;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MongoConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @PostConstruct
    public void createIndexes() {
        // seats: { eventId, zone, status }
        mongoTemplate.indexOps("seats")
            .ensureIndex(new CompoundIndexDefinition(
                new Document("eventId", 1).append("zone", 1).append("status", 1)))
            .subscribe(name -> log.info("Index created: {}", name));

        // seats: { eventId, row, seatNumber } — unique
        mongoTemplate.indexOps("seats")
            .ensureIndex(new CompoundIndexDefinition(
                new Document("eventId", 1).append("row", 1).append("seatNumber", 1))
                .unique())
            .subscribe(name -> log.info("Unique index created: {}", name));

        // events: { status }
        mongoTemplate.indexOps("events")
            .ensureIndex(new Index().on("status", Sort.Direction.ASC))
            .subscribe(name -> log.info("Index created: {}", name));

        // locations: { type }, { parentCode }
        mongoTemplate.indexOps("locations")
            .ensureIndex(new Index().on("type", Sort.Direction.ASC))
            .subscribe();
        mongoTemplate.indexOps("locations")
            .ensureIndex(new Index().on("parentCode", Sort.Direction.ASC))
            .subscribe();
    }
}
```

- [ ] **Step 2: Tạo SecurityConfig — ADMIN header check**

Event-catalog-service nằm sau API Gateway. Gateway đã validate JWT và inject `X-User-Role`. Service chỉ cần đọc header này.

```java
// infrastructure/config/SecurityConfig.java
package com.ticketing.catalog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .anyExchange().permitAll()
            )
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .build();
    }
}
```

> **Note:** Auth check cho ADMIN endpoints được thực hiện ở Controller level bằng cách đọc `X-User-Role` header. Gateway đã reject request không hợp lệ trước khi xuống đây.

- [ ] **Step 3: Tạo locations.json seed data**

```json
// src/main/resources/data/locations.json
{
  "provinces": [
    {"code": "HN", "name": "Hà Nội"},
    {"code": "HCM", "name": "TP. Hồ Chí Minh"},
    {"code": "DN", "name": "Đà Nẵng"},
    {"code": "HP", "name": "Hải Phòng"},
    {"code": "CT", "name": "Cần Thơ"},
    {"code": "BD", "name": "Bình Dương"},
    {"code": "BH", "name": "Bình Thuận"},
    {"code": "LA", "name": "Long An"},
    {"code": "DL", "name": "Đà Lạt"},
    {"code": "KH", "name": "Khánh Hòa"}
  ],
  "districts": [
    {"code": "HN-HK", "name": "Hoàn Kiếm", "parentCode": "HN"},
    {"code": "HN-BD", "name": "Ba Đình", "parentCode": "HN"},
    {"code": "HN-DDA", "name": "Đống Đa", "parentCode": "HN"},
    {"code": "HN-HBT", "name": "Hai Bà Trưng", "parentCode": "HN"},
    {"code": "HN-CG", "name": "Cầu Giấy", "parentCode": "HN"},
    {"code": "HCM-Q1", "name": "Quận 1", "parentCode": "HCM"},
    {"code": "HCM-Q3", "name": "Quận 3", "parentCode": "HCM"},
    {"code": "HCM-Q7", "name": "Quận 7", "parentCode": "HCM"},
    {"code": "HCM-BT", "name": "Bình Thạnh", "parentCode": "HCM"},
    {"code": "HCM-TD", "name": "Thủ Đức", "parentCode": "HCM"},
    {"code": "DN-HC", "name": "Hải Châu", "parentCode": "DN"},
    {"code": "DN-ST", "name": "Sơn Trà", "parentCode": "DN"},
    {"code": "DN-NT", "name": "Ngũ Hành Sơn", "parentCode": "DN"},
    {"code": "HP-HB", "name": "Hồng Bàng", "parentCode": "HP"},
    {"code": "HP-NK", "name": "Ngô Quyền", "parentCode": "HP"}
  ]
}
```

> **Note:** File này chỉ có 10 tỉnh mẫu. Để production, bổ sung đủ 63 tỉnh/thành + toàn bộ quận/huyện từ nguồn dữ liệu chính thức (ví dụ: https://provinces.open-api.vn).

- [ ] **Step 4: Tạo LocationDataSeeder**

```java
// infrastructure/config/LocationDataSeeder.java
package com.ticketing.catalog.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.catalog.domain.model.Location;
import com.ticketing.catalog.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationDataSeeder {

    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        locationRepository.count()
            .flatMap(count -> {
                if (count > 0) {
                    log.info("Locations already seeded ({} records), skipping", count);
                    return Mono.empty();
                }
                return seedFromFile();
            })
            .subscribe(
                null,
                e -> log.error("Failed to seed locations: {}", e.getMessage())
            );
    }

    private Mono<Void> seedFromFile() {
        try {
            var resource = new ClassPathResource("data/locations.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());

            List<Location> locations = new ArrayList<>();

            root.get("provinces").forEach(node -> locations.add(
                Location.builder()
                    .type("PROVINCE")
                    .code(node.get("code").asText())
                    .name(node.get("name").asText())
                    .build()
            ));

            root.get("districts").forEach(node -> locations.add(
                Location.builder()
                    .type("DISTRICT")
                    .code(node.get("code").asText())
                    .name(node.get("name").asText())
                    .parentCode(node.get("parentCode").asText())
                    .build()
            ));

            return locationRepository.saveAll(locations)
                .then()
                .doOnSuccess(v -> log.info("Seeded {} locations", locations.size()));

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to read locations.json", e));
        }
    }
}
```

- [ ] **Step 5: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/infrastructure/config/ event-catalog-service/src/main/resources/data/
git commit -m "feat(event-catalog): add MongoDB config, security config, location seeder"
```

---

## Task 7: LocationService + LocationController + Tests

**Files:**
- Create: `application/service/LocationService.java`
- Create: `api/controller/LocationController.java`
- Create: `src/test/.../service/LocationServiceTest.java`

- [ ] **Step 1: Viết failing test cho LocationService**

```java
// src/test/java/com/ticketing/catalog/service/LocationServiceTest.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.application.service.LocationService;
import com.ticketing.catalog.domain.model.Location;
import com.ticketing.catalog.domain.repository.LocationRepository;
import com.ticketing.common.cache.CacheStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock LocationRepository locationRepository;
    @Mock CacheStrategy<String, Object> cacheStrategy;
    @InjectMocks LocationService locationService;

    @Test
    void shouldReturnProvinces() {
        var province = Location.builder()
            .type("PROVINCE").code("HN").name("Hà Nội").build();

        when(locationRepository.findByType("PROVINCE")).thenReturn(Flux.just(province));

        StepVerifier.create(locationService.getProvinces())
            .expectNextMatches(r -> r.code().equals("HN") && r.name().equals("Hà Nội"))
            .verifyComplete();
    }

    @Test
    void shouldReturnDistrictsByProvince() {
        var district = Location.builder()
            .type("DISTRICT").code("HN-HK").name("Hoàn Kiếm").parentCode("HN").build();

        when(locationRepository.findByParentCode("HN")).thenReturn(Flux.just(district));

        StepVerifier.create(locationService.getDistrictsByProvince("HN"))
            .expectNextMatches(r -> r.code().equals("HN-HK"))
            .verifyComplete();
    }
}
```

- [ ] **Step 2: Chạy test — verify FAIL**

```bash
./mvnw test -pl event-catalog-service -Dtest=LocationServiceTest -DskipTests=false
```

Expected: FAIL — `LocationService` chưa tồn tại.

- [ ] **Step 3: Implement LocationService**

```java
// application/service/LocationService.java
package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.response.LocationResponse;
import com.ticketing.catalog.domain.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    public Flux<LocationResponse> getProvinces() {
        return locationRepository.findByType("PROVINCE")
            .map(l -> new LocationResponse(l.getCode(), l.getName(), null));
    }

    public Flux<LocationResponse> getDistrictsByProvince(String provinceCode) {
        return locationRepository.findByParentCode(provinceCode)
            .map(l -> new LocationResponse(l.getCode(), l.getName(), l.getParentCode()));
    }
}
```

- [ ] **Step 4: Chạy test — verify PASS**

```bash
./mvnw test -pl event-catalog-service -Dtest=LocationServiceTest -DskipTests=false
```

Expected: PASS

- [ ] **Step 5: Implement LocationController**

```java
// api/controller/LocationController.java
package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.response.LocationResponse;
import com.ticketing.catalog.application.service.LocationService;
import com.ticketing.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/provinces")
    public Mono<ApiResponse<List<LocationResponse>>> getProvinces() {
        return locationService.getProvinces()
            .collectList()
            .map(ApiResponse::success);
    }

    @GetMapping("/provinces/{code}/districts")
    public Mono<ApiResponse<List<LocationResponse>>> getDistricts(@PathVariable String code) {
        return locationService.getDistrictsByProvince(code)
            .collectList()
            .map(ApiResponse::success);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/application/service/LocationService.java event-catalog-service/src/main/java/com/ticketing/catalog/api/controller/LocationController.java event-catalog-service/src/test/
git commit -m "feat(event-catalog): add LocationService and LocationController with tests"
```

---

## Task 8: VenueService + VenueController + Tests

**Files:**
- Create: `application/service/VenueService.java`
- Create: `api/controller/VenueController.java`
- Create: `src/test/.../service/VenueServiceTest.java`

- [ ] **Step 1: Viết failing tests cho VenueService**

```java
// src/test/java/com/ticketing/catalog/service/VenueServiceTest.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.application.service.VenueService;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Venue;
import com.ticketing.catalog.domain.model.ZoneConfig;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.VenueMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

    @Mock VenueRepository venueRepository;
    @Mock VenueMapper venueMapper;
    @InjectMocks VenueService venueService;

    private Venue sampleVenue() {
        return Venue.builder()
            .id("v1").name("Nhà hát Lớn")
            .provinceCode("HN").provinceName("Hà Nội")
            .districtCode("HN-HK").districtName("Hoàn Kiếm")
            .streetAddress("1 Tràng Tiền")
            .totalCapacity(500)
            .createdAt(LocalDateTime.now())
            .zones(List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .build();
    }

    @Test
    void shouldCreateVenue() {
        var request = new CreateVenueRequest("Nhà hát Lớn", "HN", "Hà Nội",
            "HN-HK", "Hoàn Kiếm", "1 Tràng Tiền", 21.02, 105.84,
            List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")));

        var venue = sampleVenue();
        var response = new VenueResponse("v1", "Nhà hát Lớn", "HN", "Hà Nội",
            "HN-HK", "Hoàn Kiếm", "1 Tràng Tiền", 21.02, 105.84, 500,
            venue.getZones(), venue.getCreatedAt());

        when(venueMapper.toEntity(request)).thenReturn(venue);
        when(venueRepository.save(any())).thenReturn(Mono.just(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        StepVerifier.create(venueService.createVenue(request))
            .expectNextMatches(r -> r.name().equals("Nhà hát Lớn"))
            .verifyComplete();
    }

    @Test
    void shouldThrowWhenVenueNotFound() {
        when(venueRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(venueService.findById("not-exist"))
            .expectError(VenueNotFoundException.class)
            .verify();
    }
}
```

- [ ] **Step 2: Chạy test — verify FAIL**

```bash
./mvnw test -pl event-catalog-service -Dtest=VenueServiceTest -DskipTests=false
```

Expected: FAIL

- [ ] **Step 3: Implement VenueService**

```java
// application/service/VenueService.java
package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.VenueMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public Mono<VenueResponse> createVenue(CreateVenueRequest request) {
        var venue = venueMapper.toEntity(request);
        venue.setCreatedAt(LocalDateTime.now());
        return venueRepository.save(venue).map(venueMapper::toResponse);
    }

    public Mono<VenueResponse> findById(String id) {
        return venueRepository.findById(id)
            .switchIfEmpty(Mono.error(new VenueNotFoundException(id)))
            .map(venueMapper::toResponse);
    }

    public Flux<VenueResponse> findAll(String provinceCode) {
        var flux = (provinceCode != null && !provinceCode.isBlank())
            ? venueRepository.findByProvinceCode(provinceCode)
            : venueRepository.findAll();
        return flux.map(venueMapper::toResponse);
    }

    public Mono<VenueResponse> updateVenue(String id, CreateVenueRequest request) {
        return venueRepository.findById(id)
            .switchIfEmpty(Mono.error(new VenueNotFoundException(id)))
            .flatMap(existing -> {
                var updated = venueMapper.toEntity(request);
                updated.setId(existing.getId());
                updated.setCreatedAt(existing.getCreatedAt());
                return venueRepository.save(updated);
            })
            .map(venueMapper::toResponse);
    }
}
```

- [ ] **Step 4: Chạy test — verify PASS**

```bash
./mvnw test -pl event-catalog-service -Dtest=VenueServiceTest -DskipTests=false
```

Expected: PASS

- [ ] **Step 5: Implement VenueController**

```java
// api/controller/VenueController.java
package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.request.CreateVenueRequest;
import com.ticketing.catalog.api.dto.response.VenueResponse;
import com.ticketing.catalog.application.service.VenueService;
import com.ticketing.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<VenueResponse>>> createVenue(
            @Valid @RequestBody CreateVenueRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return venueService.createVenue(request)
            .map(v -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(v)));
    }

    @GetMapping
    public Mono<ApiResponse<List<VenueResponse>>> listVenues(
            @RequestParam(required = false) String provinceCode) {
        return venueService.findAll(provinceCode).collectList().map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<VenueResponse>> getVenue(@PathVariable String id) {
        return venueService.findById(id).map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<VenueResponse>>> updateVenue(
            @PathVariable String id,
            @Valid @RequestBody CreateVenueRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return venueService.updateVenue(id, request)
            .map(v -> ResponseEntity.ok(ApiResponse.success(v)));
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/application/service/VenueService.java event-catalog-service/src/main/java/com/ticketing/catalog/api/controller/VenueController.java event-catalog-service/src/test/
git commit -m "feat(event-catalog): add VenueService and VenueController with tests"
```

---

## Task 9: SeatService + Tests

**Files:**
- Create: `application/service/SeatService.java`
- Create: `src/test/.../service/SeatServiceTest.java`

- [ ] **Step 1: Viết failing tests**

```java
// src/test/java/com/ticketing/catalog/service/SeatServiceTest.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import com.ticketing.catalog.domain.repository.SeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceTest {

    @Mock SeatRepository seatRepository;
    @InjectMocks SeatService seatService;

    @Test
    void shouldGenerateCorrectSeatCount() {
        // 2 zones: VIP(2 rows × 3 seats) + FLOOR(1 row × 5 seats) = 11 seats total
        var zones = List.of(
            new EventZone("VIP", 2, 3, BigDecimal.valueOf(500000), "A"),
            new EventZone("FLOOR", 1, 5, BigDecimal.valueOf(200000), "C")
        );

        when(seatRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Seat> seats = inv.getArgument(0);
            return Flux.fromIterable(seats);
        });

        StepVerifier.create(seatService.generateSeats("event1", zones).count())
            .expectNext(11L)
            .verifyComplete();
    }

    @Test
    void shouldGenerateCorrectSeatCodes() {
        var zones = List.of(
            new EventZone("VIP", 1, 3, BigDecimal.valueOf(500000), "A")
        );

        ArgumentCaptor<List<Seat>> captor = ArgumentCaptor.forClass(List.class);
        when(seatRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Seat> seats = inv.getArgument(0);
            return Flux.fromIterable(seats);
        });

        StepVerifier.create(seatService.generateSeats("event1", zones).collectList())
            .assertNext(seats -> {
                assertThat(seats).hasSize(3);
                assertThat(seats.get(0).getCode()).isEqualTo("A1");
                assertThat(seats.get(1).getCode()).isEqualTo("A2");
                assertThat(seats.get(2).getCode()).isEqualTo("A3");
                assertThat(seats.get(0).getStatus()).isEqualTo(SeatStatus.AVAILABLE);
                assertThat(seats.get(0).getZone()).isEqualTo("VIP");
                assertThat(seats.get(0).getRow()).isEqualTo("A");
            })
            .verifyComplete();
    }

    @Test
    void shouldUpdateSeatStatusToLocked() {
        var seat = Seat.builder().id("s1").status(SeatStatus.AVAILABLE).build();
        var updated = Seat.builder().id("s1").status(SeatStatus.LOCKED).build();

        when(seatRepository.findById("s1")).thenReturn(Mono.just(seat));
        when(seatRepository.save(any())).thenReturn(Mono.just(updated));

        StepVerifier.create(seatService.updateStatus("s1", SeatStatus.LOCKED))
            .expectNextMatches(s -> s.getStatus() == SeatStatus.LOCKED)
            .verifyComplete();
    }
}
```

- [ ] **Step 2: Chạy test — verify FAIL**

```bash
./mvnw test -pl event-catalog-service -Dtest=SeatServiceTest -DskipTests=false
```

Expected: FAIL

- [ ] **Step 3: Implement SeatService**

```java
// application/service/SeatService.java
package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.response.SeatResponse;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Seat;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import com.ticketing.catalog.domain.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;

    public Flux<Seat> generateSeats(String eventId, List<EventZone> zones) {
        List<Seat> seats = new ArrayList<>();
        for (EventZone zone : zones) {
            for (int r = 0; r < zone.rows(); r++) {
                String row = zone.rowPrefix() + (char) ('A' + r - (zone.rowPrefix().charAt(0) - 'A'));
                // Compute row label: rowPrefix is base, each zone row increments from it
                String rowLabel = String.valueOf((char) (zone.rowPrefix().charAt(0) + r));
                for (int s = 1; s <= zone.seatsPerRow(); s++) {
                    seats.add(Seat.builder()
                        .eventId(eventId)
                        .zone(zone.name())
                        .row(rowLabel)
                        .seatNumber(s)
                        .code(rowLabel + s)
                        .price(zone.price())
                        .status(SeatStatus.AVAILABLE)
                        .build());
                }
            }
        }
        return seatRepository.saveAll(seats);
    }

    public Mono<Map<String, List<SeatResponse>>> findByEvent(String eventId, String zone) {
        Flux<Seat> seats = (zone != null && !zone.isBlank())
            ? seatRepository.findByEventIdAndZone(eventId, zone)
            : seatRepository.findByEventId(eventId);

        return seats
            .map(s -> new SeatResponse(s.getId(), s.getZone(), s.getRow(),
                s.getSeatNumber(), s.getCode(), s.getPrice(), s.getStatus()))
            .collectMultimap(SeatResponse::row)
            .map(m -> m.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new ArrayList<>(e.getValue())
                )));
    }

    public Mono<Seat> updateStatus(String seatId, SeatStatus status) {
        return seatRepository.findById(seatId)
            .flatMap(seat -> {
                seat.setStatus(status);
                return seatRepository.save(seat);
            });
    }

    public Mono<Void> deleteByEvent(String eventId) {
        return seatRepository.deleteByEventId(eventId);
    }
}
```

- [ ] **Step 4: Chạy test — verify PASS**

```bash
./mvnw test -pl event-catalog-service -Dtest=SeatServiceTest -DskipTests=false
```

Expected: PASS

- [ ] **Step 5: Tạo SeatController**

```java
// api/controller/SeatController.java
package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.response.SeatResponse;
import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events/{eventId}/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public Mono<ApiResponse<Map<String, List<SeatResponse>>>> getSeats(
            @PathVariable String eventId,
            @RequestParam(required = false) String zone) {
        return seatService.findByEvent(eventId, zone).map(ApiResponse::success);
    }
}
```

- [ ] **Step 6: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/application/service/SeatService.java event-catalog-service/src/main/java/com/ticketing/catalog/api/controller/SeatController.java event-catalog-service/src/test/
git commit -m "feat(event-catalog): add SeatService and SeatController with tests"
```

---

## Task 10: EventService (CRUD) + Tests

**Files:**
- Create: `application/service/EventService.java`
- Create: `src/test/.../service/EventServiceTest.java`

- [ ] **Step 1: Viết failing tests — EventService CRUD**

```java
// src/test/java/com/ticketing/catalog/service/EventServiceTest.java
package com.ticketing.catalog.service;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.common.exception.InvalidStatusTransitionException;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.EventZone;
import com.ticketing.catalog.domain.model.Venue;
import com.ticketing.catalog.domain.model.ZoneConfig;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.catalog.domain.repository.EventRepository;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.EventMapper;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.messaging.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock EventRepository eventRepository;
    @Mock VenueRepository venueRepository;
    @Mock EventMapper eventMapper;
    @Mock SeatService seatService;
    @Mock EventPublisher<Object> eventPublisher;
    @InjectMocks EventService eventService;

    private Venue sampleVenue() {
        return Venue.builder().id("v1").name("Nhà hát Lớn")
            .zones(List.of(new ZoneConfig("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .build();
    }

    private Event sampleEvent(EventStatus status) {
        return Event.builder()
            .id("e1").title("Concert A").type(EventType.CONCERT)
            .status(status).venueId("v1").venueName("Nhà hát Lớn")
            .zones(List.of(new EventZone("VIP", 5, 10, BigDecimal.valueOf(500000), "A")))
            .startTime(LocalDateTime.now().plusDays(7))
            .endTime(LocalDateTime.now().plusDays(7).plusHours(3))
            .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
            .build();
    }

    @Test
    void shouldCreateEventWithDraftStatus() {
        var request = new CreateEventRequest("Concert A", "Desc", EventType.CONCERT,
            "v1", LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(new EventZone("VIP", 5, 10, BigDecimal.valueOf(500000), "A")), null);

        var event = sampleEvent(EventStatus.DRAFT);
        var response = new EventResponse("e1", "Concert A", "Desc", EventType.CONCERT,
            EventStatus.DRAFT, "v1", "Nhà hát Lớn",
            event.getStartTime(), event.getEndTime(), event.getZones(),
            List.of(), null, event.getCreatedAt(), event.getUpdatedAt());

        when(venueRepository.findById("v1")).thenReturn(Mono.just(sampleVenue()));
        when(eventMapper.toEntity(request)).thenReturn(event);
        when(eventRepository.save(any())).thenReturn(Mono.just(event));
        when(eventMapper.toResponse(event)).thenReturn(response);

        StepVerifier.create(eventService.createEvent(request, "user1"))
            .expectNextMatches(r -> r.status() == EventStatus.DRAFT && r.title().equals("Concert A"))
            .verifyComplete();
    }

    @Test
    void shouldThrowWhenVenueNotFoundOnCreate() {
        var request = new CreateEventRequest("Concert A", "Desc", EventType.CONCERT,
            "not-exist", LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(), null);

        when(venueRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(eventService.createEvent(request, "user1"))
            .expectError(VenueNotFoundException.class)
            .verify();
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findById("not-exist")).thenReturn(Mono.empty());

        StepVerifier.create(eventService.findById("not-exist"))
            .expectError(EventNotFoundException.class)
            .verify();
    }

    @Test
    void shouldThrowWhenInvalidTransitionOnSaleToDraft() {
        var event = sampleEvent(EventStatus.ON_SALE);
        var request = new UpdateEventStatusRequest(EventStatus.DRAFT);

        when(eventRepository.findById("e1")).thenReturn(Mono.just(event));

        StepVerifier.create(eventService.updateStatus("e1", request))
            .expectError(InvalidStatusTransitionException.class)
            .verify();
    }
}
```

- [ ] **Step 2: Chạy test — verify FAIL**

```bash
./mvnw test -pl event-catalog-service -Dtest=EventServiceTest -DskipTests=false
```

Expected: FAIL

- [ ] **Step 3: Implement EventService**

```java
// application/service/EventService.java
package com.ticketing.catalog.application.service;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.common.exception.InvalidStatusTransitionException;
import com.ticketing.catalog.common.exception.VenueNotFoundException;
import com.ticketing.catalog.domain.model.Event;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.catalog.domain.repository.EventRepository;
import com.ticketing.catalog.domain.repository.VenueRepository;
import com.ticketing.catalog.infrastructure.mapper.EventMapper;
import com.ticketing.common.dto.constants.Constants;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.exception.domain.ConflictException;
import com.ticketing.common.messaging.DomainEvent;
import com.ticketing.common.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final Set<EventStatus> ALLOWED_UPDATE_STATUSES = Set.of(EventStatus.DRAFT);

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final EventMapper eventMapper;
    private final SeatService seatService;
    private final EventPublisher<Object> eventPublisher;

    public Mono<EventResponse> createEvent(CreateEventRequest request, String createdBy) {
        return venueRepository.findById(request.venueId())
            .switchIfEmpty(Mono.error(new VenueNotFoundException(request.venueId())))
            .flatMap(venue -> {
                var event = eventMapper.toEntity(request);
                event.setVenueName(venue.getName());
                event.setStatus(EventStatus.DRAFT);
                event.setCreatedBy(createdBy);
                event.setCreatedAt(LocalDateTime.now());
                event.setUpdatedAt(LocalDateTime.now());
                return eventRepository.save(event);
            })
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> findById(String id) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .map(eventMapper::toResponse);
    }

    public Flux<EventSummaryResponse> findAll(EventType type, EventStatus status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Flux<Event> flux;
        if (type != null && status != null) {
            flux = eventRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            flux = eventRepository.findByType(type, pageable);
        } else if (status != null) {
            flux = eventRepository.findByStatus(status, pageable);
        } else {
            flux = eventRepository.findAllBy(pageable);
        }
        return flux.map(eventMapper::toSummaryResponse);
    }

    public Mono<EventResponse> updateEvent(String id, UpdateEventRequest request, String userId) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> {
                if (!ALLOWED_UPDATE_STATUSES.contains(event.getStatus())) {
                    return Mono.error(new ConflictException(
                        "Event can only be updated when DRAFT, current: " + event.getStatus()));
                }
                event.setTitle(request.title());
                event.setDescription(request.description());
                event.setStartTime(request.startTime());
                event.setEndTime(request.endTime());
                event.setZones(request.zones());
                event.setDetail(request.detail());
                event.setUpdatedAt(LocalDateTime.now());
                return eventRepository.save(event);
            })
            .flatMap(saved -> publishEventUpdated(saved).thenReturn(saved))
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> updateStatus(String id, UpdateEventStatusRequest request) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> validateAndTransition(event, request.status()))
            .map(eventMapper::toResponse);
    }

    public Mono<EventResponse> cancelEvent(String id) {
        return eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .flatMap(event -> {
                if (event.getStatus() == EventStatus.COMPLETED) {
                    return Mono.error(new InvalidStatusTransitionException(
                        event.getStatus(), EventStatus.CANCELLED));
                }
                event.setStatus(EventStatus.CANCELLED);
                event.setUpdatedAt(LocalDateTime.now());
                return seatService.deleteByEvent(id)
                    .then(eventRepository.save(event));
            })
            .map(eventMapper::toResponse);
    }

    private Mono<Event> validateAndTransition(Event event, EventStatus newStatus) {
        boolean valid = switch (event.getStatus()) {
            case DRAFT -> newStatus == EventStatus.ON_SALE || newStatus == EventStatus.CANCELLED;
            case ON_SALE -> newStatus == EventStatus.CANCELLED || newStatus == EventStatus.COMPLETED;
            case SOLD_OUT -> newStatus == EventStatus.COMPLETED;
            default -> false;
        };

        if (!valid) {
            return Mono.error(new InvalidStatusTransitionException(event.getStatus(), newStatus));
        }

        event.setStatus(newStatus);
        event.setUpdatedAt(LocalDateTime.now());

        if (newStatus == EventStatus.ON_SALE) {
            return seatService.generateSeats(event.getId(), event.getZones())
                .then(eventRepository.save(event))
                .flatMap(saved -> publishEventPublished(saved).thenReturn(saved));
        }

        if (newStatus == EventStatus.CANCELLED) {
            return seatService.deleteByEvent(event.getId())
                .then(eventRepository.save(event));
        }

        return eventRepository.save(event);
    }

    private Mono<Void> publishEventPublished(Event event) {
        record EventPublishedPayload(String eventId, String title, String type,
            String venueId, LocalDateTime startTime, String status) {}
        var payload = new EventPublishedPayload(event.getId(), event.getTitle(),
            event.getType().name(), event.getVenueId(), event.getStartTime(),
            event.getStatus().name());
        return eventPublisher.publish(
            Constants.KafkaTopics.EVENT_PUBLISHED, event.getId(),
            DomainEvent.of("event.published", "event-catalog-service", payload));
    }

    private Mono<Void> publishEventUpdated(Event event) {
        record EventUpdatedPayload(String eventId) {}
        return eventPublisher.publish(
            Constants.KafkaTopics.EVENT_UPDATED, event.getId(),
            DomainEvent.of("event.updated", "event-catalog-service",
                new EventUpdatedPayload(event.getId())));
    }
}
```

- [ ] **Step 4: Chạy test — verify PASS**

```bash
./mvnw test -pl event-catalog-service -Dtest=EventServiceTest -DskipTests=false
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/application/service/EventService.java event-catalog-service/src/test/
git commit -m "feat(event-catalog): add EventService with status transitions and Kafka publish"
```

---

## Task 11: EventController + Controller Tests

**Files:**
- Create: `api/controller/EventController.java`
- Create: `src/test/.../controller/EventControllerTest.java`

- [ ] **Step 1: Viết failing controller tests**

```java
// src/test/java/com/ticketing/catalog/controller/EventControllerTest.java
package com.ticketing.catalog.controller;

import com.ticketing.catalog.api.controller.EventController;
import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.common.exception.EventNotFoundException;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired WebTestClient webTestClient;
    @MockBean EventService eventService;

    private EventResponse sampleResponse() {
        return new EventResponse("e1", "Concert A", "Desc", EventType.CONCERT,
            EventStatus.DRAFT, "v1", "Nhà hát Lớn",
            LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(3),
            List.of(), List.of(), null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void shouldReturn201WhenEventCreated() {
        when(eventService.createEvent(any(), eq("user1"))).thenReturn(Mono.just(sampleResponse()));

        webTestClient.post().uri("/api/v1/events")
            .header("X-User-Role", "ADMIN")
            .header("X-User-Id", "user1")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""
                {
                  "title": "Concert A",
                  "description": "Desc",
                  "type": "CONCERT",
                  "venueId": "v1",
                  "startTime": "2026-05-01T19:00:00",
                  "endTime": "2026-05-01T22:00:00",
                  "zones": [{"name":"VIP","rows":5,"seatsPerRow":10,"price":500000,"rowPrefix":"A"}],
                  "detail": {"type":"CONCERT","artists":["Artist A"],"genres":["Pop"],"ageRestriction":0}
                }
                """)
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.data.status").isEqualTo("DRAFT")
            .jsonPath("$.data.title").isEqualTo("Concert A");
    }

    @Test
    void shouldReturn403WhenNotAdmin() {
        webTestClient.post().uri("/api/v1/events")
            .header("X-User-Role", "CUSTOMER")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void shouldReturn404WhenEventNotFound() {
        when(eventService.findById("not-exist"))
            .thenReturn(Mono.error(new EventNotFoundException("not-exist")));

        webTestClient.get().uri("/api/v1/events/not-exist")
            .exchange()
            .expectStatus().isNotFound();
    }
}
```

- [ ] **Step 2: Chạy test — verify FAIL**

```bash
./mvnw test -pl event-catalog-service -Dtest=EventControllerTest -DskipTests=false
```

Expected: FAIL — `EventController` chưa tồn tại.

- [ ] **Step 3: Implement EventController**

```java
// api/controller/EventController.java
package com.ticketing.catalog.api.controller;

import com.ticketing.catalog.api.dto.request.CreateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventRequest;
import com.ticketing.catalog.api.dto.request.UpdateEventStatusRequest;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.catalog.api.dto.response.EventSummaryResponse;
import com.ticketing.catalog.application.service.EventService;
import com.ticketing.catalog.domain.model.enums.EventType;
import com.ticketing.common.dto.enums.EventStatus;
import com.ticketing.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return eventService.createEvent(request, userId)
            .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(e)));
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<EventResponse>> getEvent(@PathVariable String id) {
        return eventService.findById(id).map(ApiResponse::success);
    }

    @GetMapping
    public Mono<ApiResponse<List<EventSummaryResponse>>> listEvents(
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return eventService.findAll(type, status, page, size)
            .collectList()
            .map(ApiResponse::success);
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> updateEvent(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return eventService.updateEvent(id, request, userId)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }

    @PutMapping("/{id}/status")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateEventStatusRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return eventService.updateStatus(id, request)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<EventResponse>>> cancelEvent(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equals(role)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", "Admin access required")));
        }
        return eventService.cancelEvent(id)
            .map(e -> ResponseEntity.ok(ApiResponse.success(e)));
    }
}
```

- [ ] **Step 4: Chạy test — verify PASS**

```bash
./mvnw test -pl event-catalog-service -Dtest=EventControllerTest -DskipTests=false
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/api/controller/EventController.java event-catalog-service/src/test/
git commit -m "feat(event-catalog): add EventController with WebTestClient tests"
```

---

## Task 12: Kafka Consumers

**Files:**
- Create: `infrastructure/messaging/TicketStatusConsumer.java`
- Create: `infrastructure/messaging/MediaUploadedConsumer.java`

- [ ] **Step 1: Tạo TicketStatusConsumer**

```java
// infrastructure/messaging/TicketStatusConsumer.java
package com.ticketing.catalog.infrastructure.messaging;

import com.ticketing.catalog.application.service.SeatService;
import com.ticketing.catalog.domain.model.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketStatusConsumer {

    private final SeatService seatService;

    @KafkaListener(topics = "ticket.reserved", groupId = "event-catalog-service")
    public void onTicketReserved(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.reserved received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.LOCKED)
            .doOnError(e -> log.error("Failed to lock seat {}: {}", seatId, e.getMessage()))
            .subscribe();
    }

    @KafkaListener(topics = "ticket.released", groupId = "event-catalog-service")
    public void onTicketReleased(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.released received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.AVAILABLE)
            .doOnError(e -> log.error("Failed to release seat {}: {}", seatId, e.getMessage()))
            .subscribe();
    }

    @KafkaListener(topics = "ticket.issued", groupId = "event-catalog-service")
    public void onTicketIssued(Map<String, String> payload) {
        String seatId = payload.get("seatId");
        log.info("ticket.issued received — seatId={}", seatId);
        seatService.updateStatus(seatId, SeatStatus.SOLD)
            .doOnError(e -> log.error("Failed to mark seat sold {}: {}", seatId, e.getMessage()))
            .subscribe();
    }
}
```

> **Note:** `.subscribe()` dùng ở đây vì Kafka listener là entry point (non-reactive boundary). Đây là trường hợp hợp lệ duy nhất trong codebase.

- [ ] **Step 2: Tạo MediaUploadedConsumer (skeleton)**

```java
// infrastructure/messaging/MediaUploadedConsumer.java
package com.ticketing.catalog.infrastructure.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class MediaUploadedConsumer {

    // TODO: inject EventRepository và update event.imageUrls khi media-service được build
    // Payload sẽ có: { mediaId, url, eventId, size: "thumbnail|card|banner" }

    @KafkaListener(topics = "media.uploaded", groupId = "event-catalog-service")
    public void onMediaUploaded(Map<String, String> payload) {
        log.info("media.uploaded received — eventId={}, url={}, size={}",
            payload.get("eventId"), payload.get("url"), payload.get("size"));
        // TODO: eventRepository.findById(payload.get("eventId"))
        //     .flatMap(event -> { event.getImageUrls().add(url); return save; })
        //     .subscribe();
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/infrastructure/messaging/
git commit -m "feat(event-catalog): add Kafka consumers — TicketStatus + MediaUploaded skeleton"
```

---

## Task 13: Cache Integration + EventPublisher Bean

**Files:**
- Create: `infrastructure/config/CatalogConfig.java`
- Modify: `application/service/EventService.java` — inject cache
- Modify: `application/service/LocationService.java` — inject cache

- [ ] **Step 1: Tạo CatalogConfig — EventPublisher + CacheStrategy beans**

```java
// infrastructure/config/CatalogConfig.java
package com.ticketing.catalog.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.catalog.api.dto.response.EventResponse;
import com.ticketing.common.cache.redis.RedisCacheStrategy;
import com.ticketing.common.messaging.EventPublisher;
import com.ticketing.common.messaging.kafka.KafkaEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
public class CatalogConfig {

    @Bean
    public EventPublisher<Object> eventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher<>(kafkaTemplate, "event-catalog-service");
    }

    @Bean
    public RedisCacheStrategy<EventResponse> eventCacheStrategy(
            ReactiveRedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {
        return new RedisCacheStrategy<>(redisTemplate, objectMapper, EventResponse.class);
    }
}
```

- [ ] **Step 2: Cập nhật EventService — thêm cache cho findById**

Trong `EventService.java`, inject `RedisCacheStrategy<EventResponse>` và wrap `findById`:

```java
// Thêm field vào EventService:
private final RedisCacheStrategy<EventResponse> cacheStrategy;

// Sửa findById():
public Mono<EventResponse> findById(String id) {
    String cacheKey = Constants.RedisKeys.EVENT_CACHE + id;
    return cacheStrategy.getOrLoad(cacheKey, Duration.ofMinutes(30),
        () -> eventRepository.findById(id)
            .switchIfEmpty(Mono.error(new EventNotFoundException(id)))
            .map(eventMapper::toResponse));
}

// Thêm evict vào updateEvent() và updateStatus() và cancelEvent():
// Sau khi save thành công, gọi:
.flatMap(saved -> cacheStrategy.evict(Constants.RedisKeys.EVENT_CACHE + id).thenReturn(saved))
```

- [ ] **Step 3: Chạy tất cả tests**

```bash
./mvnw test -pl event-catalog-service -DskipTests=false
```

Expected: tất cả PASS

- [ ] **Step 4: Commit**

```bash
git add event-catalog-service/src/main/java/com/ticketing/catalog/infrastructure/config/CatalogConfig.java event-catalog-service/src/main/java/com/ticketing/catalog/application/service/EventService.java
git commit -m "feat(event-catalog): add Redis cache for event detail and EventPublisher bean"
```

---

## Task 14: Full Build Verification

- [ ] **Step 1: Build toàn bộ service**

```bash
./mvnw package -pl event-catalog-service -am -DskipTests
```

Expected: `BUILD SUCCESS`, jar file tạo trong `target/`

- [ ] **Step 2: Chạy tất cả tests**

```bash
./mvnw test -pl event-catalog-service -DskipTests=false
```

Expected: tất cả tests PASS, không có lỗi

- [ ] **Step 3: Update root pom.xml — verify module đã được thêm**

Kiểm tra `pom.xml` (root) có `<module>event-catalog-service</module>`. Nếu chưa có thì thêm vào.

- [ ] **Step 4: Update PLANNING.md**

Trong `PLANNING.md`, đổi trạng thái `event-catalog-service` từ `🔴 Chưa bắt đầu` thành `🟢 Done`.

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat(event-catalog): complete event-catalog-service implementation"
```

---

## Self-Review Notes

| Spec requirement | Task |
|---|---|
| CRUD Events (Concert + Movie) | Task 10, 11 |
| Lifecycle DRAFT → ON_SALE → SOLD_OUT / CANCELLED / COMPLETED | Task 10 (validateAndTransition) |
| CRUD Venues với địa chỉ đầy đủ | Task 8 |
| Auto-generate seats khi ON_SALE | Task 9 (generateSeats), Task 10 (trigger) |
| Seat status sync từ booking-service qua Kafka | Task 12 (TicketStatusConsumer) |
| Location master data seeded lúc startup | Task 6 (LocationDataSeeder) |
| GET /api/v1/locations/provinces | Task 7 (LocationController) |
| GET /api/v1/events/{id}/seats | Task 9 (SeatController) |
| Kafka producer event.published / event.updated | Task 10, 13 (CatalogConfig bean) |
| Kafka consumer skeleton media.uploaded | Task 12 (MediaUploadedConsumer) |
| Redis cache event detail 30min | Task 13 |
| MongoDB indexes seats collection | Task 6 (MongoConfig) |
| ADMIN-only endpoints | Task 8, 11 (header check) |
| StepVerifier tests | Task 7, 8, 9, 10 |
| WebTestClient tests | Task 11 |
