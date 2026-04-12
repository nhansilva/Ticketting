# TROUBLESHOOTING.md — Ticketing Platform

> Ghi lại các lỗi đã gặp và cách fix trong quá trình build user-service + api-gateway.
> Cập nhật: 2026-04-11

---

## 1. Java version mismatch — `release version 25 not supported`

**Khi nào gặp:** Lần đầu chạy `mvn compile`

**Lỗi:**
```
error: release version 25 not supported
```

**Nguyên nhân:** Máy đang dùng JDK 22.0.1, nhưng `pom.xml` set `<java.version>25</java.version>`.

**Fix:**
```bash
winget install Azul.Zulu.25.JDK
# Set JAVA_HOME
export JAVA_HOME="/c/Program Files/Zulu/zulu-25"
# Chạy Maven với JDK 25
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn spring-boot:run -pl api-gateway
```

---

## 2. `role` field bị thiếu trong User entity

**Khi nào gặp:** Review code user-service

**Lỗi (logic):** `JwtService.generateToken()` hardcode `"USER"` thay vì đọc từ entity. `JwtAuthenticationFilter` hardcode `ROLE_USER` cho tất cả user.

**Hệ quả:** Mọi user kể cả ADMIN đều có role `USER` trong JWT → ADMIN không thể gọi admin endpoints.

**Fix:**
- Thêm `enum UserRole { CUSTOMER, ADMIN }` và field `role` vào `User.java`
- Thêm column `role VARCHAR(50) NOT NULL DEFAULT 'CUSTOMER'` vào migration SQL
- `JwtService.generateToken()` đọc `user.getRole().name()` thay vì hardcode
- `JwtAuthenticationFilter` dùng `"ROLE_" + jwtService.getRoleFromToken(token)`

---

## 3. `verifyEmail` và `resetPassword` trả về `null`

**Khi nào gặp:** Code review user-service

**Lỗi:** Cả hai method đều có `return null` placeholder. `findValidToken()` trong `VerificationTokenRepository` bị comment out.

**Fix:**
- Uncomment `findValidToken()` và `deleteExpiredTokens()` trong repository
- Implement đầy đủ `verifyEmail()`: tìm token hợp lệ → verify → update user status → mark token used
- Implement đầy đủ `resetPassword()`: validate token → encode new password → save → invalidate token

---

## 4. Jackson 3.x imports — Spring Boot 4.x breaking change

**Khi nào gặp:** Compile `GatewayErrorWebExceptionHandler.java` với Spring Boot 4.x

**Lỗi:**
```
error: package com.fasterxml.jackson.core does not exist
error: package com.fasterxml.jackson.databind does not exist
```

**Nguyên nhân:** Spring Boot 4.x chuyển sang Jackson 3.x với groupId mới: `tools.jackson.*` thay vì `com.fasterxml.jackson.*`. Đây là breaking change về package name.

**Fix ban đầu (sai hướng):**
```java
// Thay đổi imports trong GatewayErrorWebExceptionHandler.java
import tools.jackson.core.JacksonException;          // thay JsonProcessingException
import tools.jackson.databind.ObjectMapper;           // thay fasterxml ObjectMapper
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler; // moved in Boot 4.x
```

**Fix thực sự (downgrade Spring Boot):** Xem lỗi số 5.

---

## 5. Spring Cloud 2024.0.x không tương thích với Spring Boot 4.x ⭐

**Khi nào gặp:** Runtime startup api-gateway

**Chuỗi lỗi (xảy ra tuần tự, mỗi fix lộ ra lỗi tiếp):**

### 5a. `LifecycleMvcEndpointAutoConfiguration` → `WebMvcAutoConfiguration` not found
```
java.lang.ClassNotFoundException: org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
```
→ Fix: exclude `LifecycleMvcEndpointAutoConfiguration` trong `application.yml`

### 5b. `RefreshAutoConfiguration` → `HibernateJpaAutoConfiguration` not found
```
java.lang.IllegalArgumentException: Could not find class [org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration]
```
→ Fix: exclude `RefreshAutoConfiguration`

### 5c. `SimpleDiscoveryClientAutoConfiguration` → `WebServerInitializedEvent` not found
```
java.lang.ClassNotFoundException: org.springframework.boot.web.context.WebServerInitializedEvent
```
→ Fix: không thể chỉ exclude — đây là vấn đề toàn diện

**Root cause:** Spring Cloud 2024.0.x (4.2.0) được thiết kế cho Spring Boot **3.4.x**, không phải 4.x:
- Spring Boot 4.x dùng Jackson 3.x (`tools.jackson.*`)
- Spring Cloud 4.2.0 compile với Jackson 2.x (`com.fasterxml.jackson.*`)
- Binary incompatible: class files của spring-cloud-gateway-server reference `com.fasterxml.jackson.databind.ObjectMapper` không tồn tại ở runtime

**Fix đúng:** Downgrade Spring Boot từ `4.0.1` → `3.4.4`

```xml
<!-- pom.xml -->
<spring-boot.version>3.4.4</spring-boot.version>  <!-- was 4.0.1 -->

<!-- Xóa explicit version overrides (để Boot BOM quản lý) -->
<!-- Xóa: spring-data-redis-reactive.version=4.0.1 -->
<!-- Xóa: spring-kafka.version=4.0.1 -->
```

Revert `GatewayErrorWebExceptionHandler.java` về Jackson 2.x + Boot 3.x imports:
```java
import com.fasterxml.jackson.core.JsonProcessingException;  // revert
import com.fasterxml.jackson.databind.ObjectMapper;          // revert
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler; // revert
```

Xóa toàn bộ Spring Cloud autoconfigure exclusions trong `application.yml`.

---

## 6. Mockito inline mock maker không hoạt động với Java 25 ⭐

**Khi nào gặp:** Chạy tests sau khi downgrade Spring Boot 3.4.4

**Lỗi:**
```
org.mockito.exceptions.base.MockitoException: Mockito cannot mock this class: class org.springframework.data.redis.core.ReactiveRedisTemplate.
Caused by: Could not modify all classes [class java.lang.Object, interface ReactiveRedisOperations, class ReactiveRedisTemplate]
Caused by: Byte Buddy could not instrument all classes within the mock's type hierarchy
```

Sau đó trong user-service:
```
Mockito cannot mock this class: interface com.ticketing.user.repository.UserRepository.
Caused by: Could not modify all classes [interface ReactiveCrudRepository, interface UserRepository, interface Repository]
```

**Nguyên nhân:** Mockito inline mock maker dùng ByteBuddy để instrument bytecode ở runtime. Java 25's module system + Unsafe API restrictions khiến ByteBuddy không thể instrument một số classes/interfaces trong hierarchy của Spring Data repositories và Spring Redis.

**Fix 1 — RateLimitFilter (đổi sang interface):**
```java
// RateLimitFilter.java — nhận interface thay vì concrete class
private final ReactiveRedisOperations<String, String> redisTemplate;  // was ReactiveRedisTemplate
public RateLimitFilter(ReactiveRedisOperations<String, String> redisTemplate) { ... }

// RateLimitFilterTest.java
@Mock private ReactiveRedisOperations<String, String> redisTemplate;  // was ReactiveRedisTemplate
```

**Fix 2 — Subclass mock maker (cho tất cả modules):**

Tạo file `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`:
```
mock-maker-subclass
```

File này chuyển Mockito từ inline ByteBuddy instrumentation → CGLIB proxy-based subclass mocking, hoạt động tốt hơn với Java 21+ module system.

> **Lưu ý cho các service tiếp theo:** Tạo file này ngay từ đầu khi setup test.

---

## 7. RateLimitFilter logic bug — `onErrorResume` catch-all nuốt `GatewayRateLimitException`

**Khi nào gặp:** Test `shouldBlockWhenOverLimit` fail với `onComplete()` thay vì error

**Lỗi:**
```
RateLimitFilterTest.shouldBlockWhenOverLimit:70 expectation "expectError(Class)" failed 
(expected: onError(GatewayRateLimitException); actual: onComplete())
```

**Code lỗi:**
```java
.flatMap(allowed -> {
    if (!allowed) return Mono.error(new GatewayRateLimitException(...));
    return chain.filter(exchange);
})
.onErrorResume(GatewayRateLimitException.class, Mono::error)  // re-emit error
.onErrorResume(e -> chain.filter(exchange));  // BUG: catch ALL errors kể cả GatewayRateLimitException!
```

**Giải thích:** `onErrorResume(GatewayRateLimitException.class, Mono::error)` re-emit exception là `Mono.error(e)`. Nhưng operator tiếp theo `.onErrorResume(e -> ...)` (không có type filter) bắt TẤT CẢ errors kể cả exception vừa được re-emit, return `chain.filter()` (Mono.empty) → test thấy `onComplete()`.

**Fix:**
```java
.flatMap(allowed -> {
    if (!allowed) return Mono.error(new GatewayRateLimitException(...));
    return chain.filter(exchange);
})
.onErrorResume(e -> {
    if (e instanceof GatewayRateLimitException) return Mono.error(e);  // propagate
    log.warn("Rate limit Redis unavailable, failing open: {}", e.getMessage());
    return chain.filter(exchange);
});
```

---

## 8. Mockito strict stubbing — `UnnecessaryStubbing`

**Khi nào gặp:** `RateLimitFilterTest.shouldBlockWhenOverLimit` sau khi fix lỗi 7

**Lỗi:**
```
RateLimitFilterTest.shouldBlockWhenOverLimit » UnnecessaryStubbing
```

**Nguyên nhân:** `setUp()` stub `when(chain.filter(any())).thenReturn(Mono.empty())` cho tất cả tests. Nhưng trong `shouldBlockWhenOverLimit`, chain không bao giờ được gọi (request bị block) → Mockito strict mode report stub thừa.

**Fix:**
```java
// Dùng lenient() cho stub có thể không được dùng trong một số test cases
lenient().when(chain.filter(any())).thenReturn(Mono.empty());
```

---

## 9. Void method mock với `thenReturn()` — UserServiceTest

**Khi nào gặp:** Compile `UserServiceTest.java`

**Lỗi:**
```
error: 'void' type not allowed here
when(emailService.sendVerificationEmail(anyString(), anyString())).thenReturn(Mono.empty());
```

**Nguyên nhân:** `EmailService.sendVerificationEmail()` return `void`. Mockito's `when(...).thenReturn()` API không dùng được cho void method.

**Fix:**
```java
// Xóa dòng stub — Mockito default cho void method là doNothing()
// Không cần stub gì cả

// Hoặc nếu cần explicit:
doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
```

---

## 10. Missing mock cho `userRepository.findByEmail` trong register test

**Khi nào gặp:** `UserServiceTest.shouldRegisterSuccessfully` fail

**Nguyên nhân:** `UserServiceImpl.register()` gọi `sendVerificationEmail(email)` → method này internally gọi `userRepository.findByEmail(email)`. Test chỉ mock `existsByEmail` nhưng không mock `findByEmail`.

**Fix:** Thêm mock:
```java
when(userRepository.findByEmail("new@example.com")).thenReturn(Mono.just(savedUser));
```

---

## 11. `@WebFluxTest` không load `SecurityConfig` → CSRF 403

**Khi nào gặp:** `UserControllerTest` — POST /register và POST /login trả về 403

**Lỗi:**
```
Status expected:<201 CREATED> but was:<403 FORBIDDEN>
Status expected:<401 UNAUTHORIZED> but was:<403 FORBIDDEN>
```

**Nguyên nhân:** `@WebFluxTest(UserController.class)` tạo một "slice" context, không tự động load `SecurityConfig` từ package khác (`com.ticketing.user.config`). Spring Security auto-config mặc định được áp dụng với **CSRF enabled** → mọi POST không có CSRF token đều bị 403.

**Fix:** Explicitly import `SecurityConfig` (và `JwtAuthenticationFilter` mà nó phụ thuộc):
```java
@WebFluxTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class UserControllerTest { ... }

// Và mock JwtService vì JwtAuthenticationFilter cần nó
@MockitoBean
private JwtService jwtService;
```

---

## 12. `JwtService` not found khi load `@WebFluxTest` context

**Khi nào gặp:** Ngay sau khi thêm `@Import(SecurityConfig.class)`

**Lỗi:**
```
Error creating bean with name 'jwtAuthenticationFilter': Unsatisfied dependency expressed through constructor parameter 0: 
No qualifying bean of type 'com.ticketing.user.service.JwtService' available
```

**Nguyên nhân:** `JwtAuthenticationFilter` depends on `JwtService`. Trong `@WebFluxTest` slice, service beans không được tạo. Cần mock `JwtService` để context load được.

**Fix:** Thêm `@MockitoBean private JwtService jwtService;` vào test class.

---

## 13. Domain exceptions trả về 500 thay vì đúng HTTP status

**Khi nào gặp:** `UserControllerTest` — error cases trả về 500

**Lỗi:**
```
Status expected:<401 UNAUTHORIZED> but was:<500 INTERNAL_SERVER_ERROR>
Range for response status value 500 INTERNAL_SERVER_ERROR expected:<CLIENT_ERROR>
```

**Nguyên nhân (2 vấn đề):**
1. `GlobalExceptionHandler` không được load trong `@WebFluxTest` slice (package `com.ticketing.common.exception.handler`)
2. `GlobalExceptionHandler.determineHttpStatus()` không có mapping cho user-specific error codes (`EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, v.v.) → fall through về `default → 500`

**Fix 1 — Thêm user error codes vào `Constants.ErrorCodes`:**
```java
public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
public static final String INVALID_TOKEN = "INVALID_TOKEN";
```

**Fix 2 — Thêm mapping vào `GlobalExceptionHandler.determineHttpStatus()`:**
```java
case Constants.ErrorCodes.EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;          // 409
case Constants.ErrorCodes.INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;       // 401
case Constants.ErrorCodes.USER_NOT_FOUND -> HttpStatus.NOT_FOUND;               // 404
case Constants.ErrorCodes.INVALID_TOKEN -> HttpStatus.BAD_REQUEST;              // 400
```

**Fix 3 — Import `GlobalExceptionHandler` vào test:**
```java
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
```

---

## Tổng kết — Checklist cho service mới

Khi bắt đầu build một service mới, làm ngay những việc này để tránh lặp lại các lỗi trên:

- [ ] **Mockito subclass mock maker**: Tạo `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` với content `mock-maker-subclass`
- [ ] **Spring Boot version**: Dùng `3.4.4` (KHÔNG phải 4.x — chưa có Spring Cloud tương thích)
- [ ] **JAVA_HOME**: Set `JAVA_HOME="/c/Program Files/Zulu/zulu-25"` trước khi chạy Maven
- [ ] **GlobalExceptionHandler**: Map đủ error codes cho domain exceptions của service
- [ ] **@WebFluxTest**: Luôn `@Import` SecurityConfig + các `@ControllerAdvice` cần thiết
- [ ] **void method mock**: Không dùng `when(...).thenReturn()` cho void methods
- [ ] **Interface vs concrete class**: Mock interface thay vì concrete class khi có thể
- [ ] **Reactive chain error**: Tránh dùng hai `onErrorResume` liên tiếp — handler thứ hai sẽ catch error được re-emit bởi handler thứ nhất

---

*File này sẽ được cập nhật khi gặp lỗi mới trong quá trình build các service tiếp theo.*
