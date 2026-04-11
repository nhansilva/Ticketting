# API Gateway

Spring Cloud Gateway — JWT authentication, routing, rate limiting cho toàn bộ Ticketing Platform.

| | |
|---|---|
| **Port** | `8080` |
| **Spring Boot** | `3.4.4` |
| **Java** | `25` |
| **Database** | Không có (stateless) |
| **Cache** | Redis (rate limiting) |

---

## Yêu cầu

| Tool | Version | Ghi chú |
|------|---------|---------|
| JDK | 25 | [Azul Zulu 25](https://www.azul.com/downloads/) |
| Maven | 3.9+ | Hoặc dùng `./mvnw` |
| Docker & Docker Compose | 24+ | Để chạy Redis |
| Redis | 7+ | Qua Docker hoặc local |

---

## 1. Cài đặt môi trường

### Cài JDK 25 (Windows — winget)

```bash
winget install Azul.Zulu.25.JDK
```

### Set JAVA_HOME

**Windows (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-25"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

**Git Bash / WSL:**
```bash
export JAVA_HOME="/c/Program Files/Zulu/zulu-25"
export PATH="$JAVA_HOME/bin:$PATH"
```

### Kiểm tra

```bash
java -version
# Output: openjdk version "25.0.x" ...

mvn -version
# Output: Apache Maven 3.x.x ... Java version: 25 ...
```

---

## 2. Khởi động Infrastructure

API Gateway cần **Redis** để chạy rate limiting. Gateway vẫn start được nếu Redis down (fail-open), nhưng rate limiting sẽ bị tắt.

### Chỉ khởi động Redis

```bash
# Từ root của project
docker compose up redis -d
```

### Kiểm tra Redis

```bash
docker compose ps
# redis    running   0.0.0.0:6379->6379/tcp

docker exec ticketing-redis redis-cli ping
# PONG
```

### Tắt infrastructure

```bash
docker compose down
```

---

## 3. Cấu hình

Gateway đọc config từ environment variables với giá trị mặc định cho local dev. **Không cần thay đổi gì để chạy local.**

### Biến môi trường (tuỳ chỉnh nếu cần)

| Biến | Mặc định | Mô tả |
|------|----------|-------|
| `SERVER_PORT` | `8080` | Port của gateway |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_SECRET` | *(secret dài 256-bit)* | **Đổi trong production!** |
| `USER_SERVICE_URL` | `http://localhost:8081` | URL user-service |
| `EVENT_CATALOG_URL` | `http://localhost:8082` | URL event-catalog-service |
| `BOOKING_URL` | `http://localhost:8083` | URL booking-service |
| `PAYMENT_URL` | `http://localhost:8084` | URL payment-service |
| `SEARCH_URL` | `http://localhost:8086` | URL search-service |
| `CMS_URL` | `http://localhost:8087` | URL cms-service |
| `MEDIA_URL` | `http://localhost:8088` | URL media-service |

### Set env vars (tuỳ chọn)

```bash
# Git Bash
export JWT_SECRET="my-super-secret-key-at-least-256-bits-long-change-this"
export REDIS_HOST="localhost"
```

---

## 4. Chạy Application

### Cách 1 — Maven (recommended cho dev)

```bash
# Từ root của project
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn spring-boot:run -pl api-gateway

# Hoặc từ thư mục api-gateway
cd api-gateway
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn spring-boot:run
```

### Cách 2 — Build JAR rồi chạy

```bash
# Build (từ root)
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn package -pl api-gateway -DskipTests

# Chạy JAR
JAVA_HOME="/c/Program Files/Zulu/zulu-25" java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

### Cách 3 — Với custom env vars

```bash
JAVA_HOME="/c/Program Files/Zulu/zulu-25" \
JWT_SECRET="my-secret-key" \
REDIS_HOST="localhost" \
mvn spring-boot:run -pl api-gateway
```

### Output khi start thành công

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.4)

... Loaded RoutePredicateFactory [Path] ...
... Netty started on port 8080 (http)
... Started ApiGatewayApplication in X.XXX seconds
```

---

## 5. Kiểm tra hoạt động

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Response (khi Redis up):**
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

**Response (khi Redis down — gateway vẫn route được):**
```json
{
  "status": "DOWN",
  "components": {
    "redis": { "status": "DOWN", "details": { "error": "..." } }
  }
}
```

### Test JWT filter

```bash
# Protected route không có token → 401
curl -i http://localhost:8080/api/v1/bookings/my
# HTTP/1.1 401
# {"error":{"code":"AUTH_MISSING_TOKEN","message":"..."}}

# Public route (pass-through, 502 nếu user-service chưa chạy)
curl -i -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com"}'
# HTTP/1.1 502 (expected nếu user-service chưa chạy)

# Protected route với token hợp lệ
curl -i http://localhost:8080/api/v1/bookings/my \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Actuator endpoints

```bash
# Danh sách routes đang active
curl http://localhost:8080/actuator/gateway/routes | python -m json.tool

# Metrics
curl http://localhost:8080/actuator/metrics

# Info
curl http://localhost:8080/actuator/info
```

---

## 6. Chạy Tests

```bash
# Từ root
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn test -pl api-gateway

# Chạy một test class cụ thể
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn test -pl api-gateway -Dtest=JwtAuthFilterTest

# Chạy với output chi tiết
JAVA_HOME="/c/Program Files/Zulu/zulu-25" mvn test -pl api-gateway -Dsurefire.useFile=false
```

**Expected output:**
```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test Class | Test Cases | Mô tả |
|-----------|-----------|-------|
| `JwtAuthFilterTest` | 9 | Public routes bypass, valid token, expired, tampered, ADMIN role |
| `RateLimitFilterTest` | 5 | Under limit, over limit, fail open (Redis down), PER_IP, fallback to IP |

---

## 7. Routes được cấu hình

| Route | Method | Path | Auth | Rate Limit |
|-------|--------|------|------|-----------|
| user-service | ALL | `/api/v1/users/**` | JWT (public: register/login) | 100 req/min/user |
| event-catalog | GET | `/api/v1/events/**` | Không | 200 req/min/IP |
| event-catalog | POST/PUT | `/api/v1/events/**` | JWT (ADMIN) | 200 req/min/IP |
| booking | ALL | `/api/v1/bookings/**` | JWT | 30 req/min/user |
| payment | ALL | `/api/v1/payments/**` | JWT | 10 req/min/user |
| payment-webhook | POST | `/api/v1/payments/webhook/**` | Không (signature) | — |
| search | GET | `/api/v1/search/**` | Không | 300 req/min/IP |
| cms | ALL | `/api/v1/cms/**` | JWT | 50 req/min/user |
| media | ALL | `/api/v1/media/**` | JWT | 20 req/min/user |

### Public routes (không cần JWT)

```
POST /api/v1/users/register
POST /api/v1/users/login
POST /api/v1/users/refresh-token
GET  /api/v1/users/password/**
GET  /api/v1/users/verification/**
GET  /api/v1/events/**
GET  /api/v1/search/**
POST /api/v1/payments/webhook/**
GET  /actuator/**
```

---

## 8. Headers được inject

Sau khi JWT được validate, gateway inject các headers sau vào request trước khi forward xuống service:

| Header | Giá trị | Mô tả |
|--------|---------|-------|
| `X-User-Id` | UUID | ID của user từ JWT claims |
| `X-User-Role` | `CUSTOMER` hoặc `ADMIN` | Role từ JWT claims |
| `X-Correlation-Id` | UUID | Trace ID (giữ nguyên nếu client gửi kèm) |
| `X-Request-Id` | UUID mới | ID unique cho mỗi request |

---

## 9. Troubleshooting

### Gateway không start — `ClassNotFoundException`
→ Kiểm tra Spring Boot version phải là `3.4.4` (không phải 4.x):
```bash
mvn help:evaluate -Dexpression=spring-boot.version -pl api-gateway -q
```

### Redis connection failed — gateway vẫn hoạt động
Gateway dùng **fail-open**: nếu Redis down, rate limiting bị bypass nhưng routing vẫn hoạt động bình thường.

### 401 với token hợp lệ
Kiểm tra `JWT_SECRET` phải **giống hệt** với `user-service`. Nếu dùng giá trị mặc định thì không cần cấu hình thêm.

### Port 8080 đã bị dùng
```bash
# Chạy trên port khác
SERVER_PORT=8090 mvn spring-boot:run -pl api-gateway
```

### Downstream service không tìm thấy (502/503)
Bình thường khi service chưa chạy. Gateway sẽ trả về `502 Bad Gateway`. Khởi động service tương ứng.

---

## 10. Docker (Production)

```dockerfile
# api-gateway/Dockerfile (nếu có)
FROM eclipse-temurin:25-jre-alpine
COPY target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build image
docker build -t ticketing/api-gateway:latest api-gateway/

# Run container
docker run -d \
  --name api-gateway \
  -p 8080:8080 \
  -e REDIS_HOST=redis \
  -e JWT_SECRET=your-production-secret \
  -e USER_SERVICE_URL=http://user-service:8081 \
  --network ticketing-network \
  ticketing/api-gateway:latest
```
