# TriageGraph

Intelligent IT Service Management and Incident Resolution System. CS F213 Object-Oriented Programming,
BITS Pilani Hyderabad Campus, Semester I 2026–27.

*Find the duplicate. Find the root cause. Fix it once.*

## Prerequisites

JDK 21, Node 22, Docker Desktop.

## Run it locally

```bash
cp .env.example .env            # then set JWT_SECRET (openssl rand -base64 48)
docker compose up -d            # PostgreSQL 17 on :5432, Mailpit on :1025 / http://localhost:8025

cd backend && ./mvnw spring-boot:run      # API on http://localhost:8080
                                          # Swagger UI: http://localhost:8080/swagger-ui.html

cd frontend && npm ci && npm run dev      # UI on http://localhost:5173
```

Profiles: `dev` (default, PostgreSQL), `test` (H2 in memory, used by the test suite), `demo` (dev plus demo seed data).

## Demo accounts (dev and demo profiles)

Every seeded account uses the password `Demo@1234`.

| Role | Email |
|---|---|
| Employee | `dev.sharma@triagegraph.local` |
| Technician | `sneha.reddy@triagegraph.local` |
| Team lead | `ananya.iyer@triagegraph.local` |
| Administrator | `vikram.singh@triagegraph.local` |
| IT manager | `meera.joshi@triagegraph.local` |

## Tests

```bash
cd backend && ./mvnw verify      # JUnit 5 + JaCoCo report in backend/target/site/jacoco
cd frontend && npm run lint && npm run build
```

## Repository layout

```
backend/     Maven · Spring Boot 4.1 · Java 21   (in.ac.bitspilani.triagegraph.<module>)
frontend/    Vite · React · TypeScript · Tailwind
docs/        uml/ · report/ · db/ · ownership.txt
```

Who owns what: docs/ownership.txt.
