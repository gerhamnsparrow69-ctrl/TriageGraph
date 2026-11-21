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

## Notifications

Three channels, chosen per event, recipient role and priority:

- In-app: the bell in every layout updates live over Server-Sent Events, with a toast for each new
  notification. The inbox is at /employee/notifications, /technician/notifications or
  /admin/notifications.
- Email: sent over SMTP to Mailpit. Open http://localhost:8025 to read them.
- Phone push: posted to the user's ntfy topic. Install the ntfy app (Android or iOS) and subscribe
  to the topic; P1 alerts arrive with maximum priority. Demo users get triagegraph-<first>-<last>
  (for example triagegraph-dev-sharma); an administrator can change it with PUT /api/v1/admin/users/{id}.
  These demo topics are easy to guess, so use long random topics for anything real.

Each send is retried 3 times with backoff; the result (SENT, FAILED or SKIPPED) is in the delivery
log at GET /api/v1/admin/notifications/log. To check every channel, sign in as the administrator,
open the Command Centre and use "Send test" on any user.

Settings live under `app.notify` in application.yml (email-enabled, push-enabled, retry-attempts,
retry-backoff, ntfy-base-url).

## Command Centre

/admin is a live view for administrators and the IT manager: headline numbers and a feed of incident
events (reported, assigned, priority changes, SLA warnings and breaches, escalations, major incidents)
that updates without refreshing. The last 50 events are kept so the feed isn't empty after a reload.

## Voice input

`VoiceButton` (frontend/src/features/voice) dictates text with the browser's Web Speech API. It works
in Chrome, Edge and Safari and hides itself in browsers without support (Firefox). Use it next to any
text field:

```tsx
<VoiceButton onText={(t) => setDescription((d) => appendSpoken(d, t))} />
```

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

Report sections are in docs/report (one .txt per section) and diagrams in docs/uml.
```

Who owns what: docs/ownership.txt.
