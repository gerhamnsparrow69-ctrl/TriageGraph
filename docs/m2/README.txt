# Member 2 handoff: triage AI and technician assignment

This page covers what Member 2 built, how to check it, and the small integration steps the other members
need to take. The full technical write-up for the report is `docs/report/m2-intelligent-features.txt`.
The test plan is `docs/test-cases.txt`.

## 1. What is in this change

| Area | Folder | Contents |
|---|---|---|
| Backend: triage | `backend/.../triage/` | category and service suggestion (keywords + similar incidents + optional Gemini), semantic duplicate detection, repeat-submission guard, join and merge, suggested resolutions, knowledge-base search, admin tuning, live accuracy evaluation |
| Backend: assignment | `backend/.../assignment/` | 4 strategies (SMART, SKILL_BASED, LOAD_BALANCED, ROUND_ROBIN) switchable at runtime, eligibility rules, fix-history ledger, workload views, reassignment, strategy comparison |
| Backend: seed | `backend/.../seed/HistoricalIncidentSeeder.java` | 60 resolved campus incidents for the knowledge base (profiles `dev`, `demo`) |
| Backend: data | `backend/src/main/resources/triage/` | `historical-incidents.tsv` (60 incidents), `duplicate-pairs.tsv` (30 labelled pairs) |
| Backend: config | `application.yml` | appended `app.triage` and `app.assignment` blocks only |
| Backend: tests | `backend/src/test/.../triage`, `.../assignment` | 138 test methods |
| Frontend | `frontend/src/features/triage`, `assignment`, `settings` | report page, resolution assistant, knowledge search, workload, team workload, admin settings |
| Frontend: routes | `frontend/src/app/routes.ts` | 4 spread lines, one per route group (see section 5) |
| Docs | `docs/uml/m2-*`, `docs/report/`, `docs/test-cases.txt`, `docs/m2/` | class and sequence diagrams (PlantUML sources + PNG + SVG), report sections, test plan |

No shared contract in `shared/` was changed. Every file outside Member 2's folders is listed in section 5.

## 2. How to check it

```bash
cd backend
./mvnw verify                      # all tests, including the Spring and MiniLM ones
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
cd ../frontend && npm ci && npm run build && npm run lint && npm run dev
```

- Without `GEMINI_API_KEY`, everything still works. The AI switches show "AI off", and suggestions come
  from keywords and similar incidents.
- With a key in `.env`, the Settings page shows **Gemini: ready**. You can change the model with
  `GEMINI_MODEL` (default `gemini-flash-lite-latest`).
- The first start downloads nothing. LangChain4j's MiniLM model is inside the jar and loads in about 1 to
  2 seconds.
- Swagger: `http://localhost:8080/swagger-ui.html`, sections **Triage**, **Triage admin**,
  **Assignment** and **Assignment admin**.

**Not run here.** Maven Central was blocked in my environment, so these 14 tests were written but run
for the first time on your machine:

- `TriageApiIntegrationTest` (7)
- `AssignmentApiIntegrationTest` (5)
- `MiniLmDuplicateEvaluationTest` (2)

The other 124 tests were compiled and run here against stand-ins for the Spring and JUnit APIs, and they
pass. If `./mvnw verify` reports anything in Member 2's folders, send the error output to Member 2 (Abir).

## 3. Demo script (Member 2's part of the video)

Log in with the seeded accounts. All use the password `Demo@1234`.

| Step | Account | Do | Expect |
|---|---|---|---|
| 1 | `dev.sharma@triagegraph.local` | **Report an issue**. Title `Fee payment app unavailable`; details `Students cannot complete fee transactions; the payment page fails to load since 10 am.` | Within a second: **Application · Fee Payment App**, high confidence. "Why this suggestion?" shows the keyword and similar-incident votes |
| 2 | same | Submit | The ticket is created and auto-assigned to **Sneha Reddy** with the reasons "APPLICATION skill · load … · 3 past fixes on Fee Payment App" |
| 3 | `rahul.verma@triagegraph.local` | Report `Fee payment not going through`: `The payment page will not load so I cannot pay my semester fee.` | Banner: **Looks like INC-…** (about 83% match), with **Yes, join** |
| 4 | same | Click **Yes, join** | No new ticket. Rahul now follows the incident, and the affected-user count rises (Member 1's command) |
| 5 | same | Report `Fee receipt download shows error 500` | Only "possibly related", no banner: same service, different problem |
| 6 | `sneha.reddy@triagegraph.local` | **My workload**, then open the incident → **Resolution assistant** | Similar past fixes (HIST-…) and a suggested fix; with Gemini on, numbered steps citing the HIST codes |
| 7 | same | **Knowledge base**: search `my account got locked right after I changed my password` | HIST-011 (accounts locked out after password change) first, about 68% similar, despite different wording |
| 8 | `vikram.singh@triagegraph.local` | **Settings → Assignment strategy**: switch to **ROUND_ROBIN**, then use **Compare strategies** | Each strategy's choice side by side; the next assignment uses round robin with no restart |
| 9 | same | **Settings → Duplicate detection**: drag the threshold | The measured F1, precision and recall chart updates (F1 0.96 at 0.62) |
| 10 | same | **Settings → Keywords**: add `canteen app` → APPLICATION, then report "canteen app crashes" | The new keyword is used immediately (zero code change) |

## 4. Integration status (merged onto main after M1, M3 and M4)

Everything below is already done in this change; it is listed so each owner can review it.

### Member 1 (incident, priority)

- `report()` already calls `checkNotRepeatSubmission`, `classify`, `index` and `autoAssign`; with Member 2's
  beans registered, the stand-ins in `incident/infrastructure/stub` switch off by themselves.
- **New: `incident/application/IncidentCommandService.java`** implements the shared `IncidentCommandPort`,
  which had no implementation yet:
  - `joinAsDuplicate`: the user follows the incident, affected users +1, priority re-assessed (can go
    P2 → P1), `DuplicateLinked` event (shows in the live feed as "Joined #…").
  - `merge`: uses the aggregate's own `mergeInto` (allowed from NEW or CLASSIFIED, as M1's states define).
  - `reassign`: uses `IncidentLifecycleService.assign`, and keeps the reason as an internal note.
  - `markSlaWarning`, `markSlaBreached`, `escalate`: only logged for now, for M3's SLA integration to fill.
- **One method added to `Incident.java`:** `joinAsAffected(userId, by, now)`, used by the join command.
- The resolution text comes from `IncidentStatusChanged.reason` ("Resolved: <summary>"); triage strips the
  prefix and stores the summary as a past fix.
- Triage never suggests a category that is not active in the catalog (the incident module would reject it).
- Frontend: `features/incident/slots.ts` has two entries, the assignment panel (team leads and admins, with
  "Assign to …" using M1's assign action) and the triage panel (similar past fixes and a link to the
  resolution assistant; hidden from employees). The report page posts exactly `IncidentRequests.Report` and
  opens the new incident afterwards.

### Member 3 (catalog, topology, SLA)

- The catalogue seed names match the 60 archived incidents exactly, so past fixes link to real services.
- `TriageApiIntegrationTest` creates the catalog rows it needs through M3's repositories (the test profile
  has no seed and the in-memory database is shared between test contexts).

### Member 4 (shared files)

- `application.yml`: `app.triage` and `app.assignment` appended after `app.notify`.
- `routes.ts`: 3 imports and 4 spread lines.
- `TechnicianLayout.tsx`: nav entries "Knowledge base" and "Team workload".
- `homes.tsx`: `TechnicianHome` ("My queue") now renders Member 2's `WorkloadPage` instead of the placeholder.
- Optional, in `.env.example`: `GEMINI_MODEL=` (default `gemini-flash-lite-latest`).
- New tables: `triage_document`, `triage_keyword`, `triage_settings`, `assignment_settings`, `technician_fix`.

## 5. Every file outside Member 2's folders

| File | Change |
|---|---|
| `backend/.../incident/application/IncidentCommandService.java` | new: implements `IncidentCommandPort` |
| `backend/.../incident/domain/Incident.java` | added `joinAsAffected` |
| `backend/.../seed/HistoricalIncidentSeeder.java` | new (order 30, after users) |
| `backend/src/main/resources/application.yml` | appended `app.triage`, `app.assignment` |
| `frontend/src/features/incident/slots.ts` | 2 side-panel entries |
| `frontend/src/app/routes.ts` | 3 imports, 4 spread lines |
| `frontend/src/app/layouts/TechnicianLayout.tsx` | 2 nav entries |
| `frontend/src/app/homes.tsx` | My queue shows the workload page |
| `docs/test-cases.txt` | new (Member 2 compiles the test plan) |

## 6. Known differences to watch

- **Model weights.** The thresholds were calibrated with the fp32 MiniLM weights. LangChain4j ships the
  quantized model, so scores can move by about ±0.02. `MiniLmDuplicateEvaluationTest` prints the real F1.
  If it is below 0.90, tell Member 2 and the threshold will be re-centred. Admins can also change it live
  on the Settings page.
- **ONNX on unusual platforms.** If the ONNX runtime cannot load, the log says "Preferred embedder
  unavailable (…); using lexical hashing (fallback)". Everything keeps working with lower recall on paraphrases.
- **Clock.** Everything uses the injected `Clock`. Tests use fixed clocks.
