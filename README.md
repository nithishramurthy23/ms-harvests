# Farm Fresh

An online store for fresh farm produce. React + Vite + Tailwind frontend, Spring Boot + MySQL backend with JWT login and email OTP.

## Requirements

- Node 18+
- Java 17 and Maven (or the wrapper in `src/mvnw`)
- MySQL 8

## Setup

1. Create the database and sample data:
   ```sh
   mysql -u root -p < Untitled.sql
   ```
2. Copy `.env.example` to `.env` and fill in the values (Gmail app password, DB password, a JWT secret from `openssl rand -base64 64`). `.env` is git-ignored; never commit it.

## Run

Backend (port 8080), from this folder so it picks up `.env`:
```sh
mvn spring-boot:run
```

Frontend (port 5173):
```sh
npm install
npm start
```

## Run everything with Docker (recommended)

Runs MySQL, two backend copies and nginx (which serves the frontend and spreads `/api` traffic across the backends):

```sh
cp .env.example .env    # fill in values
docker compose up -d --build
```

Open http://localhost:8081. To run more backend copies: `BACKEND_REPLICAS=4 docker compose up -d`.

If one backend copy crashes, nginx sends traffic to the others and Docker restarts it. Health check: `/actuator/health`.

## Configuration

| Variable | Used by | Default |
| --- | --- | --- |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | backend email (OTP, order confirmations, contact form) | none |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | backend database | `localhost:3306/farm_fresh`, `root`, none |
| `JWT_SECRET`, `JWT_EXPIRATION` | backend login tokens | none, 24h |
| `CORS_ALLOWED_ORIGINS` | backend, comma-separated frontend URLs | `http://localhost:5173,http://localhost:3000` |
| `VITE_API_URL` | frontend | `http://localhost:8080/api` |

## Project layout

- `src/` (except `src/main`): React frontend. API calls go through `src/api/axiosInstance.js`.
- `src/main/java/com/pooja/farm/fresh`: Spring Boot backend.
- `Untitled.sql`: database schema and seed data.
- `server/`: an unused Express + MongoDB prototype.
