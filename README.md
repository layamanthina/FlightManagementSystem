# Flight Management System

A production-ready microservices backend built with Java 17, Spring Boot 3.5, Spring Cloud, Spring Security, and JWT authentication.

---

## Architecture Overview

```
Client (React / Postman)
        │
        ▼
┌─────────────────┐
│   API Gateway   │  :8080  ← JWT validation, role enforcement, header injection
└────────┬────────┘
         │  Eureka Load Balancer (lb://)
    ┌────┴──────────────────────┐
    │                           │
    ▼                           ▼                        ▼
┌──────────────┐   ┌──────────────────┐   ┌──────────────────┐
│ User Service │   │ Flight Service   │   │ Booking Service  │
│    :8081     │   │    :8082         │   │    :8083         │
└──────┬───────┘   └────────┬─────────┘   └────────┬─────────┘
       │                    │                       │
       ▼                    ▼                       ▼
  user_db (MySQL)    flight_db (MySQL)       booking_db (MySQL)

All services register with:
┌──────────────────┐
│  Eureka Server   │  :8761
└──────────────────┘

Booking Service communicates with User Service and Flight Service via OpenFeign.
```

---

## Microservices

| Service         | Port | Description                                              |
|-----------------|------|----------------------------------------------------------|
| eureka-server   | 8761 | Service registry — all services register here            |
| api-gateway     | 8080 | Single entry point — JWT validation, routing             |
| user-service    | 8081 | Register, login, JWT generation, user lookup             |
| flight-service  | 8082 | Flight CRUD, search, seat management                     |
| booking-service | 8083 | Booking creation, cancellation, seat restoration         |

---

## Technology Stack

| Category          | Technology                          |
|-------------------|-------------------------------------|
| Language          | Java 17                             |
| Framework         | Spring Boot 3.5.5                   |
| Service Discovery | Spring Cloud Netflix Eureka         |
| API Gateway       | Spring Cloud Gateway (MVC)          |
| Security          | Spring Security + JWT (JJWT 0.11.5) |
| Persistence       | Spring Data JPA / Hibernate         |
| Database          | MySQL 8                             |
| Inter-service     | OpenFeign                           |
| Validation        | Jakarta Bean Validation             |
| Logging           | SLF4J / Logback                     |
| API Docs          | SpringDoc OpenAPI 2.6 (Swagger UI)  |
| Build             | Maven                               |
| Utilities         | Lombok                              |
| Testing           | JUnit 5 + Mockito                   |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8+
- Git

---

## Database Setup

Run the SQL scripts in order:

```sql
-- 1. User database
mysql -u root -p < database/user_db.sql

-- 2. Flight database
mysql -u root -p < database/flight_db.sql

-- 3. Booking database
mysql -u root -p < database/booking_db.sql
```

Or manually in MySQL Workbench / CLI:

```sql
CREATE DATABASE user_db;
CREATE DATABASE flight_db;
CREATE DATABASE booking_db;
```

---

## Environment Variables

Copy `.env.example` to `.env` and set your values:

```bash
cp .env.example .env
```

| Variable           | Description                        | Default                                          |
|--------------------|------------------------------------|--------------------------------------------------|
| DB_USERNAME        | MySQL username                     | root                                             |
| DB_PASSWORD        | MySQL password                     | root                                             |
| JWT_SECRET         | JWT signing secret (min 32 chars)  | flightmanagementsystemsecurejwtsecretkey123456789 |
| JWT_EXPIRATION_MS  | Token expiry in milliseconds       | 86400000 (24 hours)                              |

Set environment variables before starting each service:

```bash
# Windows
set DB_USERNAME=root
set DB_PASSWORD=yourpassword
set JWT_SECRET=your_secret_key_here

# Linux / macOS
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export JWT_SECRET=your_secret_key_here
```

---

## Startup Order

Services **must** be started in this order:

```
1. eureka-server   (wait for http://localhost:8761 to be available)
2. user-service
3. flight-service
4. booking-service
5. api-gateway     (start last — depends on all services being registered)
```

### Start each service

```bash
# From each service directory:
cd eureka-server  && mvn spring-boot:run
cd user-service   && mvn spring-boot:run
cd flight-service && mvn spring-boot:run
cd booking-service && mvn spring-boot:run
cd api-gateway    && mvn spring-boot:run
```

---

## Swagger / OpenAPI URLs

Access Swagger UI directly on each service (bypass the gateway):

| Service         | Swagger UI URL                              |
|-----------------|---------------------------------------------|
| user-service    | http://localhost:8081/swagger-ui.html       |
| flight-service  | http://localhost:8082/swagger-ui.html       |
| booking-service | http://localhost:8083/swagger-ui.html       |
| Eureka Dashboard| http://localhost:8761                       |

---

## API Endpoints

All requests go through the API Gateway at `http://localhost:8080`.

### Authentication (Public)

| Method | Endpoint               | Description              |
|--------|------------------------|--------------------------|
| POST   | /api/auth/register     | Register a new customer  |
| POST   | /api/auth/login        | Login and receive JWT    |

**Register Request:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "role": "CUSTOMER"
}
```

**Login Request:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Login Response:**
```json
{
  "token": "<JWT>",
  "role": "CUSTOMER",
  "message": "Login successful"
}
```

---

### Flights (Protected)

| Method | Endpoint               | Role     | Description              |
|--------|------------------------|----------|--------------------------|
| POST   | /api/flights           | ADMIN    | Create a flight          |
| GET    | /api/flights           | ANY      | List all flights         |
| GET    | /api/flights/{id}      | ANY      | Get flight by ID         |
| GET    | /api/flights/search    | ANY      | Search flights           |
| PUT    | /api/flights/{id}      | ADMIN    | Update a flight          |
| DELETE | /api/flights/{id}      | ADMIN    | Cancel a flight          |

**Search Query Parameters:** `source`, `destination`, `date` (yyyy-MM-dd)

---

### Bookings (Protected — CUSTOMER)

| Method | Endpoint               | Description                        |
|--------|------------------------|------------------------------------|
| POST   | /api/bookings          | Create a booking                   |
| GET    | /api/bookings/{id}     | Get booking by ID (own only)       |
| GET    | /api/bookings/my       | Get all bookings for logged-in user|
| DELETE | /api/bookings/{id}     | Cancel own booking                 |

**Create Booking Request:**
```json
{
  "flightId": 1,
  "numberOfSeats": 2
}
```

---

## JWT Authentication Flow

```
1. Client → POST /api/auth/login → API Gateway → User Service
2. User Service validates credentials, generates JWT with email + role claim
3. JWT returned to client
4. Client sends: Authorization: Bearer <token> on all protected requests
5. API Gateway intercepts request, validates JWT signature and expiry
6. Gateway enforces ADMIN-only routes (flight write operations)
7. Gateway injects X-User-Email and X-User-Role headers into downstream request
8. Downstream services use X-User-Email to identify the caller
```

---

## Business Rules

- Flight number must be unique per departure time
- Source and destination cannot be the same
- Departure time must be before arrival time
- Available seats = total seats on creation
- Cannot book a CANCELLED or COMPLETED flight
- Cannot book a flight whose departure time has passed
- Requested seats must not exceed available seats
- Concurrent bookings are protected by pessimistic locking
- Cancelling a booking restores seats to the flight
- Customers can only view and cancel their own bookings

---

## Running Tests

```bash
# Run all tests for a service
cd user-service    && mvn test
cd flight-service  && mvn test
cd booking-service && mvn test
```

### Test Coverage

| Service         | Test Class              | Scenarios Covered                                      |
|-----------------|-------------------------|--------------------------------------------------------|
| user-service    | AuthServiceImplTest     | Register, duplicate email, login, wrong password       |
| flight-service  | FlightServiceImplTest   | Create, duplicate, same src/dest, cancel, seat ops     |
| booking-service | BookingServiceImplTest  | Create, cancelled flight, insufficient seats, cancel   |

---

## Project Structure

```
FlightManagementSystem/
├── eureka-server/
├── api-gateway/
│   └── src/main/java/.../
│       ├── config/SecurityConfig.java
│       ├── filter/JwtAuthFilter.java
│       └── security/JwtUtil.java
├── user-service/
│   └── src/main/java/.../
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── enums/
│       ├── exception/
│       ├── repository/
│       ├── security/
│       └── service/
├── flight-service/
│   └── src/main/java/.../
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── enums/
│       ├── exception/
│       ├── repository/
│       └── service/
├── booking-service/
│   └── src/main/java/.../
│       ├── client/
│       ├── config/
│       ├── controller/
│       ├── dto/
│       ├── entity/
│       ├── enums/
│       ├── exception/
│       ├── repository/
│       └── service/
└── database/
    ├── user_db.sql
    ├── flight_db.sql
    └── booking_db.sql
```

---

## Security Notes

- Passwords are hashed with BCrypt — never stored in plain text
- JWT tokens are never logged
- Database credentials are externalized via environment variables
- JWT secret is externalized via environment variable `JWT_SECRET`
- Log injection is prevented by sanitizing user-controlled input before logging
- CSRF is disabled — this is a stateless REST API using JWT (no session cookies)
