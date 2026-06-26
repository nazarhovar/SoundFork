# SoundFork

A collaborative music production platform — like GitHub for music producers. Create projects, upload tracks, manage versions, fork other people's projects, and submit merge requests to combine your work.

Built with **Spring Boot 3.5**, featuring event-driven microservices, JWT authentication, Redis caching, and Apache Kafka for asynchronous notifications.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA, Hibernate 7 |
| Database | PostgreSQL 17 (main + notifications) |
| Cache | Redis 7 (Lettuce, GenericJackson2JsonRedisSerializer, 10-min TTL) |
| Messaging | Apache Kafka, Zookeeper |
| Auth | Custom JWT (HMAC-SHA256) |
| API | REST, SpringDoc OpenAPI (Swagger UI) |
| Build | Maven, Lombok |
| Frontend | Vanilla JS, CSS custom properties (dark theme) |
| Infrastructure | Docker Compose |

---

## Architecture

```
┌──────────────────────┐         ┌─────────────────────────┐
│   SoundFork App      │  Kafka  │   Notification Service  │
│   (:8080)            │────────▶│   (:8081)               │
│   Spring Boot + JPA  │◀── REST │   Spring Boot + JPA     │
└──────────┬───────────┘         └────────────┬────────────┘
           │                                   │
           ▼                                   ▼
┌──────────────────────┐         ┌─────────────────────────┐
│   PostgreSQL (main)  │         │   PostgreSQL (notifs)   │
│   soundfork DB       │         │   soundfork_notifs DB   │
└──────────────────────┘         └─────────────────────────┘
           │
           ▼
┌──────────────────────┐
│   Redis 7            │
│   (project cache)    │
└──────────────────────┘
```

---

## Features

- **User management** — registration, login, JWT-based auth, role-based access (USER/ADMIN), avatar upload with auto-resize
- **Projects** — CRUD with cover images, pagination, Redis-cached listing, fork with source tracking
- **Tracks** — upload/download audio files (mp3, wav, flac, ogg, aac, wma, m4a), format validation, BPM and musical key metadata
- **Versions** — snapshot-based versioning: each version captures the exact set of tracks at a point in time, with parent-version tracking
- **Merge requests** — propose changes from one project to another; approval automatically copies the source version and all its tracks into the target project
- **Notifications** — event-driven via Kafka, dedicated microservice with its own database
- **Email** — SMTP-based notifications on registration and merge request approval (Gmail)
- **Caching** — Redis-backed project listing with automatic eviction on state-changing operations (create, update, delete, fork, version changes, merge approvals)
- **Audio player** — in-browser playback of uploaded tracks
- **Dark theme** — custom CSS variables, smooth dark-only UI

---

## Quick Start

### Prerequisites

- Java 17+
- Docker Desktop (for PostgreSQL, Redis, Kafka)
- Maven (or use `mvnw`)

### Run

```bash
# Start infrastructure
docker compose up -d

# Build and run
./mvnw spring-boot:run
```

Open `http://localhost:8080`, register a new account, and start creating projects.

### Full Deployment

```bash
# Build all services and start with infrastructure
docker compose up -d --build
```

---

## API Overview

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/auth/register` | Register a new user |
| `POST` | `/auth/login` | Login, receive JWT |
| `GET` | `/users` | List users (admin) |
| `POST` | `/users` | Create user (admin) |
| `GET` | `/projects` | List projects (paginated, cached) |
| `POST` | `/projects` | Create project |
| `PUT` | `/projects/{id}` | Update project |
| `DELETE` | `/projects/{id}` | Delete project |
| `POST` | `/projects/{id}/fork` | Fork project |
| `GET` | `/projects/{id}/tracks` | List project tracks |
| `POST` | `/projects/{id}/tracks` | Upload track |
| `GET` | `/projects/{id}/versions` | Version history |
| `POST` | `/projects/{id}/versions` | Create version |
| `POST` | `/projects/{id}/merge-requests` | Create merge request |
| `POST` | `/merge-requests/{id}/approve` | Approve merge request |
| `POST` | `/merge-requests/{id}/reject` | Reject merge request |
| `GET` | `/notifications` | Get notifications |

Full API docs at `/swagger-ui.html` when running.

---

## Project Structure

```
src/main/java/com/SoundFork/SoundFork/
├── auth/             JWT auth (filter, util, controller, service)
├── config/           Security, Redis, JPA, app config
├── common/           Shared DTOs, enums, email, exceptions, image utils
├── user/             Users (entity, controller, service)
├── project/          Projects (entity, controller, service, fork logic)
├── track/            Tracks (entity, controller, service, file upload)
├── version/          Versions (entity, controller, service, track snapshots)
├── mergerequest/     Merge requests (entity, controller, service, approval)
├── notification/     Kafka event producer, REST proxy
└── SoundForkApplication.java

notification-service/   Dedicated microservice for notifications
├── src/                Spring Boot app, own DB, Kafka consumer
└── Dockerfile

nginx/                  Nginx config for production (SSL, reverse proxy)
docker-compose.yml      All services (DB x2, Redis, Kafka, ZK, apps)
deploy.sh               Production deployment script
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/soundfork` | Main database |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `root` | Database password |
| `JWT_SECRET` | `soundfork-secret-key-change-in-production` | JWT signing key |
| `CACHE_TYPE` | `redis` | Cache provider (`redis` or `simple`) |
| `REDIS_HOST` | `localhost` | Redis host |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `MAIL_USERNAME` | *(empty)* | Gmail SMTP user |
| `MAIL_PASSWORD` | *(empty)* | Gmail app password |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8081` | Notification service |

---

## Entity Model

- **User** — id, username, email, password, role, avatar, social links
- **Project** — id, title, description, genre, cover, author, sourceProject (for forks), forkedAt
- **Track** — id, title, file, fileSize, format, bpm, musicalKey, project
- **Version** — id, project, parentVersion, versionNumber, commitMessage, author
- **VersionTrack** — join table linking versions to tracks
- **MergeRequest** — id, sourceVersion, targetProject, author, status (PENDING/APPROVED/REJECTED), message
- **Notification** — id, user, type, message, isRead, createdAt

---

## License

Portfolio project.
