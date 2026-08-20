# Changelog

File ghi lại những thay đổi của dự án.
Định dạng dựa theo [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Unreleased]

### 2026-08-20 - Booking Status Email Notification

**Người thực hiện:** [Kaio]

#### Added

- Template HTML Thymeleaf thông báo trạng thái Booking thay đổi
- `BookingService.changeStatus(...)` chuẩn hóa và lưu trạng thái trước khi gửi email cho người đặt
- `BookingRepository` tải sẵn user và space phục vụ nội dung email trong transaction
- Unit test cho nội dung template, trigger gửi email và trường hợp trạng thái không đổi

#### Changed

- Không lưu lại hoặc gửi email trùng khi trạng thái Booking không thay đổi

### 2026-08-20 - Sign Up and Password Reset Email Integration

**Người thực hiện:** [Kaio]

#### Added

- Endpoint `POST /api/auth/signup` tạo user `INACTIVE`, mã hóa mật khẩu, gán role `USER` và gửi OTP xác nhận
- Endpoint `POST /api/auth/forgot-password` gửi OTP reset cho tài khoản `ACTIVE` và luôn trả `202 Accepted`
- Template HTML Thymeleaf riêng cho email xác nhận tài khoản và reset password
- Unit test cho auth service, OTP reset, template email và controller

#### Changed

- Chuẩn hóa email trước khi tra cứu và dùng thời hạn OTP từ cấu hình trong nội dung email

#### Fixed

- Cho phép Spring Boot xử lý `/error` để validation và lỗi email của các API auth không bị chuyển thành `403 Forbidden`

### 2026-08-20 - Send Account Confirmation OTP API

**Người thực hiện:** [Kaio]

#### Added

- Endpoint `POST /api/auth/send-confirm` nhận email hợp lệ và trả `202 Accepted`
- Sinh OTP 6 chữ số bằng `SecureRandom`, hash trước khi lưu và hết hạn sau 5 phút
- Lưu OTP xác nhận theo user, thay thế OTP cũ và gửi mã qua `EmailService`
- Unit test cho OTP generator, service và controller

### 2026-08-20 - Base Email Service

**Người thực hiện:** [Kaio]

#### Added

- Cấu hình SMTP qua biến môi trường và file cấu hình local
- `EmailService` hỗ trợ gửi email plain text và HTML qua `JavaMailSender`
- Xử lý lỗi gửi mail tập trung bằng `EmailSendingException`
- Unit test cho nội dung email, validation và lỗi SMTP

### 2026-08-20 - Unit test for JwtAuthenticationFilter

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- `JwtAuthenticationFilterTest`: 14 unit test cho `JwtAuthenticationFilter` dùng Mockito
  - Không có token / header sai prefix → SecurityContext rỗng, filter chain vẫn được gọi
  - Token không hợp lệ / hết hạn → không gọi `loadUserByUsername`, không set Authentication
  - Token hợp lệ → Authentication set đúng principal, authorities và credentials null
  - User nhiều role → tất cả role đều có trong Authentication

---

### 2026-08-20 - Fix CORS allowed-origins config format

**Người thực hiện:** [Huỳnh Trương Thảo Duyên]

#### Fixed

- `application.yml`: sửa `app.cors.allowed-origins` từ dạng YAML list sang chuỗi phân tách bởi dấu phẩy (`http://localhost:3000, http://localhost:5173`) — `SecurityConfig.corsAllowedOrigins` dùng `@Value("${app.cors.allowed-origins}")` bind vào `List<String>`, chỉ parse đúng khi giá trị là chuỗi comma-separated chứ không phải YAML list nhiều dòng

---

### 2026-08-20 - Security Layer (JWT + Spring Security)

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- `SecurityConfig`: cấu hình `SecurityFilterChain`, phân quyền URL, CORS, `BCryptPasswordEncoder`, `DaoAuthenticationProvider`
- `JwtTokenProvider`: generate/validate access token (1 ngày) và refresh token (7 ngày)
- `JwtAuthenticationFilter`: xác thực JWT mỗi request, set `SecurityContext`
- `CustomUserDetailsService`: load user từ DB, map `Set<Role>` → `List<GrantedAuthority>`
- `JwtProperties`: bind JWT config từ `application.yml` qua `@ConfigurationProperties`

#### Changed

- `application.yml`: thêm config `app.jwt` (secret từ env `JWT_SECRET`), `app.cors.allowed-origins`

---

**Người thực hiện:** [Trịnh Yến Nhi]

#### Added

- Kết nối database Supabase (PostgreSQL) qua HikariCP
- Tạo 8 JPA Entity theo ERD thiết kế:
  - `Role`
  - `Amenity`
  - `User`
  - `Venue`
  - `Space`
  - `Booking`
  - `Payment`
  - `Message`
- Cấu hình i18n với ngôn ngữ mặc định tiếng Việt (`messages_vi.properties`)
- Cấu hình Swagger UI tại `/swagger-ui.html`

#### Changed

- Tên bảng `user` → `users` (tránh reserved keyword trong PostgreSQL)
- `ddl-auto: update` để Hibernate tự tạo/cập nhật bảng

#### Notes

- Bảng junction được JPA tự tạo: `user_roles`, `venue_amenities`, `space_host`
- Các thay đổi so với ERD gốc: xem [Entity Design Decisions](#entity-design-decisions)

---

## Entity Design Decisions

| #   | Chỗ thay đổi           | ERD gốc         | Code thực tế                  | Lý do                                       |
| --- | ---------------------- | --------------- | ----------------------------- | ------------------------------------------- |
| 1   | Bảng User              | `user`          | `users`                       | `user` là reserved keyword trong PostgreSQL |
| 2   | latitude / longitude   | `decimal(10,8)` | `BigDecimal`                  | Tránh sai số float                          |
| 3   | description            | `text`          | `columnDefinition = "TEXT"`   | JPA mặc định dùng VARCHAR(255)              |
| 4   | capacity               | `int`           | `Integer`                     | Wrapper class hỗ trợ giá trị null           |
| 5   | open_time / close_time | `time`          | `LocalTime`                   | Java type mapping cho PostgreSQL time       |
| 6   | Các timestamp          | `timestamp`     | `LocalDateTime`               | Java type mapping cho PostgreSQL timestamp  |
| 7   | payment.booking_id     | FK              | `@OneToOne` + `unique = true` | Đảm bảo ràng buộc 1-1 ở tầng DB             |

---

_Template cho các lần cập nhật tiếp theo:_

```
## [Unreleased]

### YYYY-MM-DD - [Tên tính năng]

**Người thực hiện:** [Tên thành viên]

#### Added
- ...

#### Changed
- ...

#### Fixed
- ...

#### Removed
- ...
```
