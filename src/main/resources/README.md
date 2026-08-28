# Resource Booking System

A secure RESTful API for booking resources (rooms, vehicles, equipment) built with Spring Boot, Spring Security, JWT authentication, and role-based access control (RBAC).

## Tech Stack

- Java 17+ (tested on 21)
- Spring Boot 3.2.3
- Spring Security + JWT (jjwt 0.12.5)
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven

## Features

- JWT-based authentication (`POST /auth/login`)
- Role-based access control: `ROLE_ADMIN`, `ROLE_USER`
  - **ADMIN**: full CRUD on resources and reservations
  - **USER**: read-only on resources; create/view/cancel own reservations only
- Reservation lifecycle: `PENDING` → `CONFIRMED` / `CANCELLED`
- Reservation price stored as `BigDecimal` (scale 2), calculated from resource hourly rate × duration
- Filtering reservations by `status`, `minPrice`, `maxPrice`
- Pagination (`page`, `size`) and sorting (`sortBy`, `sortDir`)
- User identity always resolved server-side from the JWT — never trusted from the request body
- Global exception handling with structured error responses (400/401/403/404/500)
- API documentation via Swagger/OpenAPI

## Prerequisites

- JDK 17+
- Maven 3.8+
- PostgreSQL 14+ running locally or accessible remotely

## Environment Variables

| Variable | Description | Example |
|---|---|---|
| `DB_PASSWORD` | Database password | `yourStrongDbPassword` |
| `JWT_SECRET` | Secret key used to sign JWTs (required, no default) | Generate with `openssl rand -hex 32` |
| `JWT_EXPIRATION` | Token validity in ms (optional, default `3600000` = 1 hour) | `3600000` |

**PowerShell:**
```powershell
$env:DB_PASSWORD="yourStrongDbPassword"
$env:JWT_SECRET="<your generated secret>"
$env:JWT_EXPIRATION="3600000"
```

**Linux/macOS:**
```bash
export DB_PASSWORD=yourStrongDbPassword
export JWT_SECRET=$(openssl rand -hex 32)
export JWT_EXPIRATION=3600000
```

## Database Setup

```sql
CREATE DATABASE booking_db;
```

Schema is auto-managed via `spring.jpa.hibernate.ddl-auto=update` on startup — no manual migrations needed.

Default connection settings in `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/booking_db
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
```

## Running the Application

```bash
mvn clean install
mvn spring-boot:run
```

Runs on `http://localhost:8080`.

## Seed Users

Created automatically on first startup:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` |
| `user` | `user123` | `ROLE_USER` |

## Authentication

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

Use on subsequent requests:
```http
Authorization: Bearer <token>
```

## API Endpoints

### Resources
| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/resources` | Any authenticated user |
| GET | `/api/resources/{id}` | Any authenticated user |
| POST | `/api/resources` | ADMIN only |
| PUT | `/api/resources/{id}` | ADMIN only |
| DELETE | `/api/resources/{id}` | ADMIN only |

### Reservations
| Method | Endpoint | Access |
|---|---|---|
| POST | `/api/reservations` | Any authenticated user (creates for self) |
| GET | `/api/reservations` | ADMIN sees all; USER sees own only |
| GET | `/api/reservations/{id}` | ADMIN any; USER own only |
| PUT | `/api/reservations/{id}/cancel` | ADMIN any; USER own only |
| PUT | `/api/reservations/{id}/confirm` | ADMIN only |
| DELETE | `/api/reservations/{id}` | ADMIN only |

**Filtering, sorting & pagination example:**
GET /api/reservations?status=CONFIRMED&minPrice=50&maxPrice=200&page=0&size=5&sortBy=price&sortDir=desc


## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`

## Error Responses

| Status | Cause |
|---|---|
| 400 | Validation failure / invalid booking (e.g. end time before start time) |
| 401 | Missing/invalid/expired token, or wrong login credentials |
| 403 | Insufficient role, or accessing another user's reservation |
| 404 | Resource/reservation not found |
| 500 | Unexpected server error |

## Security Notes

- Passwords hashed with BCrypt.
- `JWT_SECRET` must be supplied via environment variable — app fails to start if unset (no insecure default).
- Reservation ownership is always derived from the authenticated JWT principal, never from the request body.