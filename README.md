# 🎬 TicketFlow — Event Booking & Seat Management Engine

> A **production-grade movie ticket booking backend** built with Java Spring Boot — engineered to handle real-world problems like concurrent seat reservation, expired lock cleanup, waitlist management, real email notifications, Redis caching, and fraud-safe Stripe payments.
>
> Built to solve the same engineering challenges that power **BookMyShow, Ticketmaster, and PVR Cinemas** at scale.

---

## 🚀 Why This Project Stands Out

| Engineering Challenge | Solution Used |
|---|---|
| Two users booking the same seat simultaneously | Pessimistic locking (`PESSIMISTIC_WRITE`) on seat reservation |
| Locked seats never released if payment fails | `@Scheduled` job every minute — auto-releases locks older than 10 min |
| Notifications must not slow down booking API | `Spring Application Events` + `@Async` — fully decoupled, non-blocking |
| Email must be persisted even if sending fails | Save to DB first → send → update status (SENT/FAILED) |
| Sold-out shows need a fair queue | Full **Waitlist system** — position tracking, notify first in queue on cancellation |
| Frequent show/movie queries hitting DB every time | **Redis caching** with `@Cacheable` / `@CacheEvict` + 15-min TTL |
| Payment confirmed by frontend = fraud risk | **Stripe webhook** — only server-side event confirms booking |
| Business needs revenue and occupancy reports | **Analytics APIs** — revenue, top movies, peak hours, occupancy % |

---

## 🏗️ Architecture

```
Client Request
      │
      ▼
JWT Auth Filter ──── validates token, injects User into SecurityContext
      │
      ▼
Controller Layer ──── 13 controllers, 44 REST APIs
      │
      ▼
Service Layer ──── business logic, @Transactional, Spring Events
      │         │
      │         ├── ApplicationEventPublisher
      │         │     └── BookingConfirmedEvent / BookingCancelledEvent
      │         │           └── NotificationServiceImpl (@EventListener + @Async)
      │         │                 └── JavaMailSender → real emails
      │         │
      │         └── SeatLockExpiryService (@Scheduled every minute)
      │
      ▼
Repository Layer ──── JPA + custom JPQL, pessimistic locks, aggregate queries
      │
      ▼
PostgreSQL ──── 10 entities, unique constraints, DB-level integrity
      │
Redis ──── show/movie search cache (15-min TTL)
      │
Stripe API ──── Checkout session + Webhook + Refund
```

---

## ✨ Key Features

### 👤 User
- Signup / Login / Token refresh with **JWT** (access + refresh tokens)
- View profile and booking history
- Receive **real email notifications** on booking confirmation and cancellation

### 🎬 Movies & Shows
- Browse movies filtered by **city and genre** — paginated
- Search shows by **city, movie, and date** — paginated
- View **real-time seat map** showing AVAILABLE / LOCKED / BOOKED seats

### 🎟️ Booking Flow
```
User selects seats
      │
      ▼
POST /bookings/initiate ──── PESSIMISTIC lock acquired on seats
      │                       Seats status → LOCKED (10-min timer starts)
      ▼
POST /bookings/{id}/payment ── Stripe Checkout session created
      │
      ▼
User pays on Stripe
      │
      ▼
Stripe → POST /webhook/payment ── validates Stripe-Signature header
      │
      ▼
Seats → BOOKED, Booking → CONFIRMED ── @Async email sent to user
```
> ⚠️ Booking is **NEVER confirmed from frontend**. Only the verified Stripe webhook confirms it.

### ⏱️ Seat Lock Expiry — Automatic Cleanup
```java
@Scheduled(cron = "0 * * * * *")  // every minute
@Transactional
public void releaseExpiredLocks() {
    showSeatRepository.releaseExpiredLocks(LocalDateTime.now().minusMinutes(10));
}
```
A single bulk `UPDATE` query releases all expired locks every minute. No loop, no N+1. If a user doesn't pay within 10 minutes, their seats are automatically freed for others.

### 📋 Waitlist System
- Join/leave waitlist for any sold-out show
- Track your **position in the queue**
- When a booking is cancelled → first person in waitlist is **automatically notified**
- Statuses: `WAITING → NOTIFIED → CONVERTED → EXPIRED`

### 📧 Real Email Notifications (JavaMailSender)
Three notification types sent as actual emails:
- **Booking Confirmed** — movie name, date, time, seats, total amount, booking ID
- **Booking Cancelled** — refund amount and timeline
- **Waitlist Available** — seats freed, urgency message with 15-minute window

All emails are persisted to DB first (`PENDING`), sent, then status updated to `SENT` or `FAILED` — ensuring no notification is ever lost silently.

### 📊 Analytics (Admin)
- **Revenue report** — total revenue and avg per booking for any date range
- **Show report** — occupancy %, booked vs available seats, revenue per show
- **Top movies by revenue** — paginated, date-filtered
- **Peak booking hours** — `GROUP BY HOUR` aggregate query
- **Venue report** — per-venue analytics

### 🗄️ Redis Caching
```java
@Cacheable(value = "movies", key = "#id")           // cache movie by ID
@Cacheable(value = "showSearch", key = "...")        // cache search results
@CacheEvict(value = {"movies", "moviesList"}, ...)  // invalidate on update/delete
```
Custom `CacheConfig` with 15-minute TTL, `JavaTimeModule` for `LocalDate/LocalTime` serialization, and `DefaultTyping.NON_FINAL` for polymorphic types — done correctly.

---

## 🧠 Engineering Deep Dive

### 1. Pessimistic Locking — Preventing Double Booking
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT ss FROM ShowSeat ss WHERE ss.show.id = :showId AND ss.id IN :seatIds")
List<ShowSeat> findAndLockSeats(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);
```
When two users try to book the same seat simultaneously, the database row is locked for the duration of the transaction. The second request waits, then finds the seat is `LOCKED` and throws an error — preventing double booking at the database level.

### 2. Spring Application Events — Decoupled Notifications
```java
// BookingService fires the event — knows nothing about notifications
eventPublisher.publishEvent(new BookingConfirmedEvent(booking));

// NotificationService listens — completely independent
@EventListener
@Async  // runs on separate thread — doesn't block booking response
public void sendBookingConfirmation(Booking booking) {
    saveAndSendEmail(booking.getUser(), ...);
}
```
The booking API returns in milliseconds. Email sending happens asynchronously on a separate thread. Adding a new notification type requires zero changes to `BookingService`.

### 3. Auto-Generated ShowSeats on Show Creation
When admin creates a show, the system automatically creates one `ShowSeat` row per physical seat in the screen — priced by seat type:
```java
BigDecimal price = switch (seat.getType()) {
    case PREMIUM  -> request.getPricePremium();
    case VIP, RECLINER -> request.getPriceVip();
    default       -> request.getPriceRegular();
};
```
No manual seat assignment needed. Every seat in the screen is immediately available for booking.

### 4. JWT Exception Handling in Security Filter
JWT validation errors occur inside the filter — before `@RestControllerAdvice` can catch them. Fix: inject `HandlerExceptionResolver` into the filter and call `resolveException()` manually. This routes the exception through the normal handler, returning a clean JSON error response instead of an unformatted Spring error page.

---

## 📡 Complete API Reference — 44 APIs

### 🔐 Auth (3 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/auth/signup` | Register |
| POST | `/auth/login` | Login → JWT + refresh token |
| POST | `/auth/refresh` | Refresh access token |

### 🎬 Movies — Public (2 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/movies` | Browse movies (filter by city, genre, paginated) |
| GET | `/movies/{id}` | Get movie by ID (Redis cached) |

### 🎬 Movies — Admin (4 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/admin/movies` | Create movie |
| PUT | `/admin/movies/{id}` | Update movie |
| DELETE | `/admin/movies/{id}` | Delete movie |
| PATCH | `/admin/movies/{id}/activate` | Toggle active status |

### 🎭 Shows — Public (3 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/shows` | Search shows by city, movie, date (paginated, cached) |
| GET | `/shows/{id}` | Get show details |
| GET | `/shows/{id}/seats` | Get real-time seat map |

### 🎭 Shows — Admin (4 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/admin/shows` | Create show (auto-generates all ShowSeats) |
| PUT | `/admin/shows/{id}` | Update show |
| DELETE | `/admin/shows/{id}` | Delete show |
| PATCH | `/admin/shows/{id}/prices` | Update seat prices |

### 🏢 Venues — Public (2 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/venues` | Get venues by city |
| GET | `/venues/{id}` | Get venue details |

### 🏢 Venues — Admin (5 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/admin/venues` | Create venue |
| PUT | `/admin/venues/{id}` | Update venue |
| DELETE | `/admin/venues/{id}` | Delete venue |
| POST | `/admin/venues/{id}/screens` | Add screen to venue |
| GET | `/admin/venues/{id}/screens` | Get screens of venue |

### 🎪 Screens — Admin (4 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/admin/screens/{id}` | Update screen |
| DELETE | `/admin/screens/{id}` | Delete screen |
| POST | `/admin/screens/{id}/seats` | Add seats to screen |
| GET | `/admin/screens/{id}/seats` | Get seats of screen |

### 🎟️ Bookings (6 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/bookings/initiate` | Lock seats + create booking |
| POST | `/bookings/{id}/payment` | Create Stripe checkout session |
| GET | `/bookings/{id}/status` | Get booking status |
| POST | `/bookings/{id}/cancel` | Cancel + Stripe refund + notify waitlist |
| GET | `/bookings/my` | Get my booking history |
| GET | `/bookings/{id}` | Get booking details |

### 📋 Waitlist (4 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/waitlist/{showId}` | Join waitlist |
| DELETE | `/waitlist/{showId}` | Leave waitlist |
| GET | `/waitlist/{showId}/position` | My queue position |
| GET | `/waitlist/admin/{showId}` | View full waitlist (admin) |

### 🏙️ Cities (2 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/cities` | Get all cities |
| POST | `/admin/cities` | Create city |

### 📊 Analytics — Admin (5 APIs)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/admin/analytics/revenue` | Revenue report with date range |
| GET | `/admin/analytics/shows/{id}` | Show occupancy + revenue report |
| GET | `/admin/analytics/movies/top` | Top movies by revenue |
| GET | `/admin/analytics/venues/{id}` | Venue analytics |
| GET | `/admin/analytics/peak-hours` | Peak booking hours |

### 🔔 Notifications (1 API)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users/notifications` | Get my notifications |

### 👤 User (1 API)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/users/profile` | Get my profile |

### 💳 Webhook (1 API)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/webhook/payment` | Stripe webhook → confirm/cancel booking |

**Total: 44 REST APIs across 13 controllers**

---

## 🗄️ Database Design

```
city          ← Cities where venues exist
venue         ← Cinema halls per city
screen        ← Screens inside a venue (REGULAR/IMAX/4DX)
seat          ← Physical seats in a screen (REGULAR/PREMIUM/VIP/RECLINER)
movie         ← Movies with genre, duration, rating
show          ← A movie at a screen on a date + time
show_seat     ← Seat availability per show (AVAILABLE/LOCKED/BOOKED)
booking       ← User's booking record
waitlist_entry← Queue for sold-out shows
notification  ← Email notification records with status
```

**Booking Status Lifecycle:**
```
INITIATED → PAYMENT_PENDING → CONFIRMED
                           └──→ CANCELLED
```

**ShowSeat Status Lifecycle:**
```
AVAILABLE → LOCKED (10-min timer) → BOOKED
                └──→ AVAILABLE (if lock expires or booking cancelled)
```

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT (JJWT) |
| Database | PostgreSQL |
| Cache | Redis (`@Cacheable`, 15-min TTL) |
| ORM | Spring Data JPA / Hibernate |
| Payments | Stripe API (Checkout + Webhooks + Refunds) |
| Email | JavaMailSender (real SMTP emails) |
| Events | Spring Application Events + `@Async` |
| Scheduler | `@Scheduled` (seat lock expiry + show reminders) |
| Mapping | ModelMapper |
| Validation | Bean Validation (`@Valid`, `@NotNull`) |
| Build | Maven |
| Utils | Lombok |

---

## ⚙️ Local Setup

### Prerequisites
- Java 17+
- PostgreSQL
- Redis
- Maven
- Stripe account (for payment testing)
- SMTP credentials (Gmail / Mailtrap for email testing)

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/ShivamNayak-dev/TicketFlow.git
cd TicketFlow
```

**2. Create PostgreSQL database**
```sql
CREATE DATABASE ticketflow_db;
```

**3. Configure environment variables**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ticketflow_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

spring.redis.host=localhost
spring.redis.port=6379

jwt.secretKey=your_jwt_secret_key_min_32_characters

stripe.secret.key=sk_test_your_stripe_key
stripe.webhook.secret=whsec_your_webhook_secret

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_email@gmail.com
spring.mail.password=your_app_password

frontend.url=http://localhost:3000
```

**4. Run the application**
```bash
mvn spring-boot:run
```

---

## 📁 Project Structure

```
src/main/java/com/shivam/bookMyShow/
├── controller/        # 13 REST controllers — 44 APIs
├── service/           # Business logic interfaces
│   └── impl/          # Service implementations + SeatLockExpiryService
├── repository/        # JPA repos + custom JPQL queries
├── entity/            # 10 JPA entities + 9 enums
├── dto/
│   ├── request/       # 9 request DTOs with @Valid
│   └── response/      # 14 response DTOs
├── events/            # BookingConfirmedEvent, BookingCancelledEvent
├── security/          # JWTAuthFilter, JWTService, WebSecurityConfig
├── config/            # CacheConfig (Redis), StripeConfig, CorsConfig
├── advice/            # GlobalExceptionHandler + GlobalResponseHandler
└── exception/         # Custom exception classes
```

---

## 🔮 Planned Improvements

- [ ] Unit and integration tests (JUnit 5 + Mockito)
- [ ] Docker + Docker Compose setup
- [ ] Swagger / OpenAPI documentation
- [ ] QR code generation for confirmed bookings
- [ ] Seat selection UI integration guide

---

## 📄 License

MIT License — free to use, modify, and distribute.

---

*Built by [Shivam Nayak](https://github.com/ShivamNayak-dev) | [LinkedIn](https://www.linkedin.com/in/shivam-nayak-886495297/)*
