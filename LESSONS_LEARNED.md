# Lessons Learned — Event Ticketing Platform

> Format: Ngày · Service · Lỗi · Nguyên nhân · Cách fix

---

## 2026-04-11

### [user-service] Kafka deserializer ClassNotFoundException
- **Lỗi:** `ClassNotFoundException: org.apache.kafka.common.deserialization.StringDeserializer`
- **Nguyên nhân:** Typo trong `application.yml` — package sai: `deserialization` thay vì `serialization`
- **Fix:** Đổi thành `org.apache.kafka.common.serialization.StringDeserializer`
- **Note:** Cả `key-deserializer` và `value-deserializer` đều bị — cần check cả hai khi gặp lỗi này

---

### [user-service] Spring Data Redis scan nhầm R2DBC repository
- **Lỗi:** `Could not safely identify store assignment for repository candidate interface UserRepository`
- **Nguyên nhân:** Spring Boot auto-config cả Redis và R2DBC cùng lúc, Redis scan nhầm repository của R2DBC
- **Fix:** Exclude `RedisRepositoriesAutoConfiguration` khỏi `@SpringBootApplication`
  ```java
  @SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
  ```
- **Note:** Xảy ra khi project dùng cả `spring-data-redis` và `spring-data-r2dbc`

---

### [user-service] Duplicate @EnableR2dbcRepositories
- **Lỗi:** `BeanDefinitionOverrideException: Cannot register bean definition for 'userRepository'`
- **Nguyên nhân:** `@EnableR2dbcRepositories` khai báo 2 lần — cả `UserServiceApplication` và `DatabaseConfig`
- **Fix:** Chỉ giữ lại annotation ở `DatabaseConfig`, xóa khỏi main class
- **Note:** Không thêm annotation đã có ở config class vào main class

---

### [common] Common modules không có trong Maven local repo
- **Lỗi:** `Could not find artifact com.ticketing:common-entity:jar:1.0.0-SNAPSHOT`
- **Nguyên nhân:** `mvn compile` chỉ build vào `target/`, không install vào `~/.m2`. `spring-boot:run` resolve từ `~/.m2`
- **Fix:**
  ```bash
  mvn install -pl common/common-entity,common/common-base -am -DskipTests
  ```
- **Note:** Mỗi khi thay đổi common lib phải `install` lại trước khi chạy service

---

## 2026-04-12

### [user-service] CORS error 403 trên preflight request
- **Lỗi:** `CORS error` + `403 preflight` khi FE (localhost:5173) gọi BE (localhost:8081)
- **Nguyên nhân:** `allowCredentials=true` + `setAllowedOrigins("*")` không compatible — Spring reject config này
- **Fix:** Dùng `setAllowedOriginPatterns(List.of("*"))` thay vì `setAllowedOrigins`
  ```java
  config.setAllowedOriginPatterns(List.of("*"));
  config.setAllowCredentials(true);
  ```
- **Note:** Khi `allowCredentials=true` bắt buộc phải dùng `allowedOriginPatterns`, không dùng được wildcard `*` với `allowedOrigins`

---

### [user-service] Phone number validation reject số VN
- **Lỗi:** `Phone number must be valid` với số `0397338935`
- **Nguyên nhân:** Regex `^\\+?[1-9]\\d{1,14}$` yêu cầu bắt đầu bằng `[1-9]`, số VN bắt đầu bằng `0`
- **Fix:** Đổi regex thành pattern VN:
  ```java
  @Pattern(regexp = "^(\\+?84|0)[3-9]\\d{8}$")
  ```
- **Note:** Pattern này cover: `0397338935`, `+84397338935`, các đầu số `03x`, `07x`, `08x`, `09x`

---

### [user-service] R2DBC UPDATE thay vì INSERT khi entity có ID
- **Lỗi:** `Failed to update table [users]; Row with Id [...] does not exist`
- **Nguyên nhân:** Spring Data R2DBC dùng `id != null` để quyết định INSERT hay UPDATE. Khi tự generate UUID trước khi save → R2DBC tưởng là UPDATE
- **Fix:** Implement `Persistable<UUID>` trên entity, thêm field `@Transient boolean isNew`, set `isNew(true)` khi tạo entity mới
  ```java
  public class User implements Persistable<UUID> {
      @Transient
      private boolean isNew = false;

      @Override
      public boolean isNew() { return isNew; }
  }
  ```
  Và khi build:
  ```java
  User.builder().id(UUID.randomUUID()).isNew(true)...build();
  ```
- **Note:** Áp dụng cho tất cả entity dùng UUID tự generate: `User`, `UserPreferences`, `VerificationToken`

---

### [user-service] Schema bảng chưa được tạo
- **Lỗi:** `relation "users" does not exist`
- **Nguyên nhân:** File migration ở `db/migration/V1__*.sql` (Flyway format) nhưng Flyway chưa được thêm dependency. `spring.sql.init` chỉ đọc `schema.sql` ở root resources
- **Fix:**
  1. Copy file migration thành `src/main/resources/schema.sql`
  2. Config `application.yml`:
     ```yaml
     spring.sql.init:
       mode: always  # Đổi thành never sau khi schema đã tạo
       schema-locations: classpath:schema.sql
     ```
- **Note:** Sau khi bảng đã tạo xong → đổi `mode: never` để tránh chạy lại mỗi lần restart

---

### [postgres] user_db chưa tồn tại
- **Lỗi:** R2DBC không connect được — database không tồn tại
- **Nguyên nhân:** Init script `init-multiple-dbs.sh` chỉ chạy khi container khởi động **lần đầu**. Nếu container đã chạy trước khi script được thêm vào → script không chạy
- **Fix:** Tạo DB thủ công qua IntelliJ Database tool:
  ```sql
  CREATE DATABASE user_db;
  ```
- **Note:** Để tránh lần sau — xóa volume và recreate container: `docker compose down -v && docker compose up -d postgres`

---

## Template cho entry mới

```
### [service-name] Tên lỗi ngắn gọn
- **Lỗi:** Message lỗi cụ thể
- **Nguyên nhân:** Giải thích root cause
- **Fix:** Code hoặc config thay đổi
- **Note:** Điều cần nhớ để tránh lần sau
```
