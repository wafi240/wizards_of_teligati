# Wizards of Teligati — Backend Service

**Version:** 1.0.0-SNAPSHOT  
**Framework:** Spring Boot 3.2.4  
**Runtime:** Java 21  
**Build Tool:** Gradle 8.5  
**Database:** PostgreSQL 16 (Managed via Flyway)  

---

## 1. Prerequisites

- **Java Development Kit (JDK):** Version 21 (OpenJDK / Eclipse Temurin / JetBrains Runtime 21)
- **Docker & Docker Compose:** Version 24+ (for local PostgreSQL 16 container)
- **Gradle:** 8.5 (Wrapper provided via `./gradlew` / `gradlew.bat`)

---

## 2. Environment Configuration

The backend is configured to read its database and server properties from environment variables with safe development defaults.

Copy the provided template to create your local `.env` file:

```bash
cp .env.example .env
```

### Environment Variables Reference

| Variable | Description | Default / Example Value |
|---|---|---|
| `SERVER_PORT` | HTTP port the Spring Boot server listens on | `8080` |
| `DB_NAME` | PostgreSQL database name | `teligati` |
| `DB_USERNAME` | PostgreSQL database user | `postgres` |
| `DB_PASSWORD` | PostgreSQL database password | `postgres` (or secure local password) |
| `DB_PORT` | Host port mapped to PostgreSQL container | `5432` |
| `DB_URL` | JDBC connection string | `jdbc:postgresql://localhost:5432/teligati` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed client origins | `http://localhost:3000,http://localhost:8080` |

> [!CAUTION]
> Never commit `.env` or production credentials to source control. `.env` is ignored by `.gitignore`.

---

## 3. Starting the Local Database (Docker Compose)

Launch the isolated PostgreSQL 16 service with persistent storage:

```bash
# Navigate to the backend directory
cd backend

# Start the PostgreSQL service in detached mode
docker compose up -d
```

### Verification
Check the container status and automated health probe:

```bash
docker compose ps
```

The container `teligati-postgres` should report `healthy`.

To stop the database:
```bash
docker compose down
```

---

## 4. Building and Running the Backend

### Run Automated Tests
Tests execute against an isolated in-memory test database with Flyway migration and schema validation enabled:

```bash
# Windows
.\gradlew.bat test

# Linux / macOS
./gradlew test
```

### Compile & Assemble JAR
```bash
# Windows
.\gradlew.bat build

# Linux / macOS
./gradlew build
```

The production-ready executable JAR is output to `build/libs/backend-1.0.0-SNAPSHOT.jar`.

### Start the Application Server
Ensure your PostgreSQL database container is running (`docker compose up -d`), then start the server:

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

---

## 5. Verifying the Health Endpoint

The service exposes an unauthenticated health probe at:

```http
GET http://localhost:8080/api/v1/health
```

### Example Successful Response (`200 OK`)
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "database": "UP",
    "version": "1.0.0-SNAPSHOT",
    "uptimeMs": 4512
  },
  "message": "System is operational",
  "timestamp": "2026-11-06T00:15:00"
}
```

### Degraded State (`503 Service Unavailable`)
If the PostgreSQL database is unreachable, the endpoint returns:
```json
{
  "success": true,
  "data": {
    "status": "DEGRADED",
    "database": "DOWN",
    "version": "1.0.0-SNAPSHOT",
    "uptimeMs": 8120
  },
  "message": "System is running in degraded state",
  "timestamp": "2026-11-06T00:15:00"
}
```

---

## 6. Project Package Structure

```text
backend/
├── src/main/java/com/teligati/backend/
│   ├── BackendApplication.java       # Spring Boot main entry point
│   ├── common/                       # Consistent ApiResponse<T> and ApiError envelopes
│   ├── config/                       # Application configuration beans
│   ├── controller/                   # REST controllers (HealthController, ApiController)
│   ├── dto/                          # Data transfer objects (HealthResponse)
│   ├── entity/                       # JPA entities (User, Profile, Job, Project, Transaction)
│   ├── exception/                    # GlobalExceptionHandler and custom exceptions
│   ├── repository/                   # Spring Data JPA repositories
│   ├── security/                     # SecurityConfig, EntryPoint, AccessDeniedHandler
│   └── service/                      # Business logic (HealthService)
├── src/main/resources/
│   ├── application.properties        # Main application properties with env variable bindings
│   └── db/migration/
│       └── V1__init_foundation.sql   # Flyway baseline schema migration
├── src/test/                         # Integration and unit test suite
├── docker-compose.yml                # PostgreSQL 16 container definition
├── .env.example                      # Template for environment configuration
└── README.md                         # This documentation
```

---

## 7. Known Limitations of Phase 5B.1

1. **Authentication:** Phase 5B.1 sets up the Spring Security foundation (stateless, CORS, exception handlers, public health endpoint, secure-by-default protected routes). User login, registration, JWT issuance, and password verification will be implemented in **Phase 5B.2**.
2. **Business Endpoints:** Career opportunities, thesis proposals, reference requests, messaging, and notifications will be connected to database services in subsequent phases according to `docs/backend_architecture_and_api_plan.md`.
3. **Android Client:** Android source code was untouched in this phase; the client continues running locally using Room.
