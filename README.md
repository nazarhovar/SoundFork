# SoundFork 🎵

**SoundFork** is a collaborative music production platform — like GitHub for music producers. Create projects, upload tracks, manage versions, fork other people's projects, and submit merge requests to combine your changes.

Built with Spring Boot 3, featuring a microservices architecture with JWT authentication, Redis caching, Kafka event-driven notifications, and a single-page frontend.

---

## Features

- **User management** — registration, login, JWT-based auth, profile avatars
- **Projects** — create, update, delete, fork (like GitHub repos), cover images
- **Tracks** — upload/download audio files (mp3, wav, flac, ogg, aac, wma, m4a) with format validation
- **Versions** — snapshot-based versioning: each version captures the exact set of tracks at a point in time
- **Merge requests** — propose changes from one project to another; approve merges with automatic version+track copying
- **Notifications** — event-driven (via Kafka) notification system with a dedicated microservice
- **Caching** — Redis-backed caching for project listings (10-min TTL)
- **Image processing** — automatic avatar resizing (200×200) and center-crop for cover images

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA |
| **Database** | PostgreSQL 17 (x2 — main + notifications) |
| **Cache** | Redis 7 (Lettuce + GenericJackson2JsonRedisSerializer) |
| **Messaging** | Apache Kafka + Zookeeper |
| **Auth** | Custom JWT (HMAC-SHA256, no external library) |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Build** | Maven, Lombok |
| **Frontend** | Vanilla JS, CSS custom properties (dark/light theme) |
| **Containerization** | Docker Compose (PostgreSQL x2, Redis, Kafka, Zookeeper) |

---

## Architecture

```
┌─────────────┐     Kafka      ┌─────────────────────┐
│  SoundFork   │──────────────▶│  notification-service│
│  (:8080)     │◀─── REST ────│  (:8081)             │
└──────┬──────┘                └──────────┬──────────┘
       │                                   │
       ▼                                   ▼
┌──────────────┐                ┌────────────────────┐
│  PostgreSQL   │                │  PostgreSQL         │
│  soundfork    │                │  soundfork_notifs   │
│  (:5432)      │                │  (:5433)            │
└──────────────┘                └────────────────────┘
       │
       ▼
┌──────────────┐
│  Redis 7      │
│  (:6379)      │
└──────────────┘
```

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for detailed diagrams (Mermaid).

---

## Quick Start

### Prerequisites

- Java 17
- Docker Desktop (for PostgreSQL, Redis, Kafka)
- Maven (or use the included `mvnw` wrapper)

### 1. Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL (x2), Redis, Kafka, and Zookeeper.

### 2. Build and run the main service

```bash
./mvnw spring-boot:run
```

The application starts at `http://localhost:8080`.

### 3. (Optional) Run notification service

```bash
cd ../notification-service
# ensure it has its own PostgreSQL instance on port 5433
./mvnw spring-boot:run
```

### 4. Open the app

Navigate to `http://localhost:8080` — register a new account and start creating projects.

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/soundfork` | Main DB |
| `SPRING_DATASOURCE_USERNAME` | `soundfork` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `soundfork` | DB password |
| `APP_JWT_SECRET` | `soundfork-secret-key-change-in-production` | JWT signing key |
| `CACHE_TYPE` | `redis` | Cache provider (`redis` or `simple`) |
| `MAIL_USERNAME` | *(empty)* | Gmail SMTP user (for welcome/approval emails) |
| `MAIL_PASSWORD` | *(empty)* | Gmail app-specific password |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8081` | Notification microservice URL |

---

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register |
| `POST` | `/auth/login` | Login |
| `GET` | `/users` | List users |
| `GET` | `/users/{id}` | Get user |
| `POST` | `/users` | Create user (admin) |
| `PUT` | `/users/{id}` | Update user |
| `DELETE` | `/users/{id}` | Delete user (admin) |
| `GET` | `/projects` | List projects (paginated, cached) |
| `POST` | `/projects` | Create project |
| `GET` | `/projects/{id}` | Get project |
| `PUT` | `/projects/{id}` | Update project |
| `DELETE` | `/projects/{id}` | Delete project |
| `POST` | `/projects/{id}/fork` | Fork project |
| `GET` | `/projects/{id}/tracks` | List tracks |
| `POST` | `/projects/{id}/tracks` | Upload track |
| `GET` | `/projects/{id}/versions` | Version history |
| `POST` | `/projects/{id}/versions` | Create version |
| `POST` | `/projects/{id}/versions/with-track` | Create version + upload track |
| `GET` | `/projects/{id}/merge-requests` | List merge requests |
| `POST` | `/projects/{id}/merge-requests` | Create merge request |
| `POST` | `/merge-requests/{id}/approve` | Approve merge |
| `POST` | `/merge-requests/{id}/reject` | Reject merge |
| `GET` | `/notifications` | Get notifications |
| `GET` | `/notifications/unread/count` | Unread count |

Full API documentation available at `/swagger-ui.html` when the app is running.

---

## Testing

```bash
./mvnw test
```

The project includes:
- Unit tests for services (AuthService, UserService, NotificationService)
- Controller tests (AuthController, UserController) using `@WebMvcTest`
- Application context loading test
- TestContainers dependency ready for integration tests

> Note: notification-service tests require the service to be running or will log connection-refused warnings (expected in test output).

---

## Project Structure

```
src/main/java/com/SoundFork/SoundFork/
├── auth/           — JWT auth (filter, util, controller, service)
├── config/         — Security, Redis, App config
├── common/         — Shared DTOs, enums, email, exceptions, image utils, migration
├── user/           — Users (entity, controller, service, cleanup)
├── project/        — Projects (entity, controller, service, forking)
├── track/          — Tracks (entity, controller, service, file management)
├── version/        — Versions (entity, controller, service, version-track linking)
├── mergerequest/   — Merge requests (entity, controller, service, approval)
└── notification/   — Notification REST proxy, Kafka events
```

---

## License

This project is created for portfolio purposes.
