# Common Modules

Các module dùng chung cho tất cả services trong hệ thống ticketing.

## 📦 Modules

### 1. common-dto
**Mục đích**: Shared DTOs, Enums, và Constants

**Nội dung**:
- `enums/`: TicketStatus, BookingStatus, EventStatus, PaymentStatus
- `response/`: ApiResponse, ErrorResponse
- `constants/`: Constants (Redis keys, Kafka topics, Error codes, Headers)

**Sử dụng**: Tất cả services import để dùng chung DTOs và constants

---

### 2. common-exception
**Mục đích**: Custom exceptions và global exception handling

**Nội dung**:
- `exception/`: 
  - `TicketingException` (base exception)
  - `TicketNotAvailableException`
  - `TicketAlreadyReservedException`
  - `BookingNotFoundException`
  - `BookingExpiredException`
  - `InsufficientInventoryException`
  - `RateLimitExceededException`
- `handler/`: `GlobalExceptionHandler` (reactive WebFlux)

**Sử dụng**: Tất cả services extend từ các exceptions này

---

### 3. common-config
**Mục đích**: Shared configurations và beans

**Nội dung**:
- `RedisConfig`: Reactive Redis template configuration
- `WebFluxConfig`: WebClient builder, Correlation ID filter
- `KafkaConfig`: Kafka producer configuration

**Sử dụng**: Auto-configuration khi services import module này

---

### 4. common-utils
**Mục đích**: Utility classes và helper functions

**Nội dung**:
- `RedisLockUtil`: Distributed locking với Redis
  - `acquireLock()`: Acquire lock với timeout
  - `releaseLock()`: Release lock
  - `executeWithLock()`: Execute task với lock
- `RedisCacheUtil`: Redis caching utilities
  - `get()`, `set()`, `delete()`
  - `getOrCompute()`: Cache-aside pattern
- `RateLimiterUtil`: Rate limiting với Redis
  - `checkRateLimit()`: Sliding window log
  - `checkRateLimitSimple()`: Fixed window counter

**Sử dụng**: Inject vào services để dùng utilities

---

## 🔧 Build

```bash
# Build tất cả common modules
cd common
mvn clean install

# Hoặc build từ root
cd ..
mvn clean install -pl common -am
```

## 📝 Usage Example

### Trong Service POM:
```xml
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-dto</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-exception</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-config</artifactId>
    <version>${project.version}</version>
</dependency>
<dependency>
    <groupId>com.ticketing</groupId>
    <artifactId>common-utils</artifactId>
    <version>${project.version}</version>
</dependency>
```

### Trong Service Code:
```java
// Use DTOs
import com.ticketing.common.dto.enums.TicketStatus;
import com.ticketing.common.dto.response.ApiResponse;

// Use Exceptions
import com.ticketing.common.exception.TicketNotAvailableException;

// Use Utils
@Autowired
private RedisLockUtil redisLockUtil;

redisLockUtil.executeWithLock("ticket:123", () -> {
    // Critical section
    return updateTicket();
});
```

## ✅ Next Steps

Sau khi hoàn thành common modules, tiếp tục với:
1. Inventory Service (sử dụng RedisLockUtil, RedisCacheUtil)
2. Booking Service (sử dụng common-exception, common-dto)
3. API Gateway (sử dụng RateLimiterUtil)

