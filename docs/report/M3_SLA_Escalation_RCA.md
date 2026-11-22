# M3 report material — SLA, Escalation and Root-Cause Analysis

## 1. Service catalogue and topology foundation

M3 provides the service catalogue and the service dependency graph used by incident classification, prioritisation, topology exploration and RCA. The catalogue exposes service and category lookups. The application contains 17 campus services in the published demo description. The topology graph represents directed dependencies and exposes graph, dependency, dependent and blast-radius queries. The admin and technician topology pages render the graph with Cytoscape and allow a user to inspect a selected service's direct/transitive relationships and blast radius.

The direction of an edge is important: a dependent service points to the dependency it requires. The blast radius of a service is derived from the services that depend on it, directly or transitively. This makes service relationships useful beyond visualisation: impact propagation provides context to the priority and root-cause workflows.

## 2. Service-level agreements

### Targets and policy lookup

The SLA target API returns response target, resolution target, warning percentage and policy name for a selected service and priority. The default target matrix implemented by the current policy provider is:

| Priority | Response target | Resolution target | Warning |
|---|---:|---:|---:|
| P1 | 5 minutes | 30 minutes | 75% |
| P2 | 15 minutes | 2 hours | 75% |
| P3 | 1 hour | 8 hours | 75% |
| P4 | 4 hours | 24 hours | 75% |

The Fee Payment App has a more stringent P1 override of 3 minutes response and 20 minutes resolution, with the same 75% warning threshold. The current UI allows an administrator to select a service and priority and inspect the corresponding targets. The UI is a target-view page; the current backend is not a full database-backed policy CRUD console.

The target values are copied into the incident's SLA state at creation in the intended lifecycle design. This avoids retroactively changing an existing incident's contractual target when a default policy changes. The incident model contains embedded start, response, pause and due-time attributes.

### Warning and breach handling — delivered scope and limitation

M3 has a persisted `SlaClock` model, a scheduled monitor, a warning/breach report endpoint and a standalone, deterministic `SlaEvaluator` submitted as a separate PR. The evaluator accepts lifecycle status, response acknowledgement time, warning and due timestamps, pause state and previously recorded warning/breach flags. It returns response/resolution warning and breach decisions in deterministic order, suppresses already-recorded decisions, stops response evaluation after acknowledgement, and pauses resolution evaluation while `PENDING_INFO`/other paused-resolution states apply. Its focused test run with the existing SLA module suite reported 41 passing tests; the standalone evaluator is not itself evidence that the live scheduler is integrated.

The current M3 live monitor path has not yet been fully rewired to call the M1 incident command methods. The group explicitly identified that the existing `SlaClockService` publishes `SlaWarningRaised` and `SlaBreached` directly, while M1 added incident-side `markSlaWarning` and `markSlaBreached` operations that also publish the events. Connecting both publishers without removing the direct M3 publication could create duplicate alerts. Therefore live incident-level warning/breach orchestration is documented as deferred/future work, not as completed end-to-end behaviour.

The intended integration boundary is: M3 detects the due/warning transition; M3 calls the incident command port; M1 validates and records the transition in the incident aggregate and publishes one event; M4's notification/audit listeners react to that event. M3 must not publish the same incident event directly once that path is wired. The existing `openWithDeadlineBefore` query can be called with a look-ahead of `now + longest SLA target`, with M3 applying the warning-percentage threshold to the returned candidates; no query contract change is required per the team discussion.

The current SLA breach report is backed by persisted `SlaClock` warning/breach flags. It reports response breach count, resolution breach count and unique incidents with any breach. Tests cover response-only, resolution-only, both-clocks-on-one-incident counted once, empty results and admin/IT-manager access. The report must not be described as a historical violation ledger: the frozen schema uses `sla_clocks`, not a separate `SlaViolation` table.

### Lifecycle rules

The intended business behaviour is that the response clock stops when the incident is first acknowledged. `PENDING_INFO` pauses the resolution clock while awaiting the reporter; providing information resumes the remaining resolution budget. Resolution is also paused while the incident is resolved pending verification/closure. Closed and merged incidents do not continue active SLA evaluation. The older elapsed-clock implementation contains known mismatches around polling and pause tracking; the standalone evaluator encodes the corrected response-vs-resolution distinction, but the live integration remains deferred.

## 3. Escalation

M1 owns the incident aggregate and its persisted escalation level. M1's aggregate supports advancing the escalation level and raising an `IncidentEscalated` event. M4 owns the notification delivery infrastructure and handles the notification event; the notification links to the incident page. M2 owns reassignment through the existing assignment flow.

The agreed ownership rule is strict: escalation changes escalation level and sends a notification; it does not auto-assign or auto-unassign technicians. A team lead or administrator who receives the notification may decide to reassign using the existing incident-page recommendation panel and M2 assignment action. This avoids interference with M2 auto-assignment, workload limits and workload counts.

The intended escalation chain is assignee → team lead → administrator → IT manager. The aggregate-level event/persistence capability exists, but M3's complete time-driven escalation planner/monitor orchestration has not been delivered. For this submission, do not claim that the four-level timed ladder automatically progresses in the running application. Treat automated escalation scheduling as future work unless the final main branch has acquired and passed that integration after this report's source snapshot.

## 4. Root-cause analysis

### Purpose and API

RCA helps explain several affected services with one likely upstream cause. It is exposed through `POST /api/v1/rca/rank`, accepting an `affectedServiceIds` array. The endpoint is available to technicians, team leads, administrators and the IT manager.

### Candidate generation and scoring

The algorithm builds candidates from the affected services and their dependency context. For each candidate service *c*, the scorer forms its impact set from the candidate and its transitive dependents. Coverage is the share of affected services explained by that impact set; specificity is the share of the candidate's impact set that is actually affected. The implemented scoring design is `min(1, 0.60 × coverage + 0.25 × specificity + evidence bonus)`. Recent change evidence contributes up to +0.10 and an open incident on the candidate contributes up to +0.05; the total evidence bonus is capped at +0.15. The evidence aggregation is deterministic and retains the strongest contribution per evidence type rather than stacking duplicates. Candidates are sorted deterministically by rounded score, then specificity, then service identifier. The response marks ambiguity when the leading candidates are within the configured score margin (0.05 in the current design).

The RCA response returns ranked candidates and makes the top candidate, runner-up and ambiguity visible to the caller. Evidence text supports the recommendation instead of returning only an opaque score. The topology graph is reused as the structural basis for candidate impact.

### Verified demonstration scenario

The seeded scenario for affected services LMS, ERP Portal and Email & Collaboration ranks Campus SSO first with a score of approximately 0.7833, followed by LDAP Directory (0.675), Core Campus Network (0.6469), Data-Centre Power (0.6441) and Database Cluster (0.525). The tests also cover the case without a recent SSO change, where Campus SSO (approximately 0.6833) and LDAP Directory (0.675) are close enough for the response to be marked ambiguous.

The current RCA implementation incorporates recent-change and open-incident evidence. Full Major Incident correlation/creation and creation of a major incident with three child incidents are not included in the delivered RCA PR; that part of TC30 remains incomplete.

## 5. Implemented APIs and pages

- Catalogue services and categories: `/api/v1/catalog/...` and `/admin/catalog`.
- Topology graph, dependencies, dependents and blast radius: `/api/v1/topology/...`, `/admin/topology` and `/technician/topology`.
- SLA targets and breach counts: `/api/v1/sla/targets` and `/api/v1/admin/reports/sla-breaches`; target view `/admin/sla`.
- RCA ranking: `/api/v1/rca/rank`.

The Catalogue, Admin Topology, Technician Topology and SLA Policies pages have been merged to `main` according to the M4 integration update. The topology page is lazy-loaded so Cytoscape does not inflate the initial application bundle.
