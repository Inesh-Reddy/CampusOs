# CampusOS — Requirements

## 1. Problem statement
Colleges lose maintenance requests, double-book labs/classrooms, and make operational decisions without data. CampusOS centralises issue reporting, resource booking, and operational analytics in one modular monolith.

## 2. Target users & authorization matrix

| Capability | Student | Faculty | Technician | Dept Admin | Campus Admin |
|---|---|---|---|---|---|
| Create issue | ✓ | ✓ | ✓ | ✓ | ✓ |
| View own issues | ✓ | ✓ | ✓ (assigned+own) | ✓ dept scope | ✓ all |
| View all issues | – | – | assigned only | dept scope | ✓ |
| Assign/reassign technician | – | – | – | ✓ | ✓ |
| Transition issue status | own: cancel only | own: cancel only | assigned: progress/resolve | ✓ | ✓ |
| Browse/book resources | ✓ | ✓ | ✓ | ✓ | ✓ |
| Approve/reject booking | – | – | – | ✓ (approval-required types) | ✓ |
| Cancel own booking | ✓ | ✓ | ✓ | ✓ | ✓ |
| Cancel others' booking | – | – | – | ✓ | ✓ |
| Manage resources/users | – | – | – | limited | ✓ |
| View analytics | own summary | dept summary | own workload | dept | ✓ campus-wide |
| Run optimization simulator | – | ✓ | – | ✓ | ✓ |

Backend enforces every rule; frontend hiding is cosmetic only.

## 3. User stories (MVP)
- **US-1** As a student, I report an issue with category/location/priority so maintenance can act. *Accept:* valid issue → 201 + reference `ISS-####`; invalid → 400 with field errors; activity row created.
- **US-2** As a student, I see only my issues. *Accept:* another student's issue detail → 403/404; list never leaks others' rows.
- **US-3** As a technician, I progress assigned issues through valid transitions only. *Accept:* `REPORTED→IN_PROGRESS` rejected; `ASSIGNED→IN_PROGRESS→RESOLVED` (with notes) succeeds; each step audited.
- **US-4** As an admin, I assign a technician. *Accept:* student attempt → 403; valid assign sets assignee + `ASSIGNED` + activity.
- **US-5** As any user, I book a resource without overlap. *Accept:* overlapping active booking → 409 with alternatives; adjacent (end==start) succeeds.
- **US-6** As an admin, I approve/reject approval-required bookings. *Accept:* `PENDING→CONFIRMED/REJECTED`; requester notified via status.
- **US-7** As an admin, I view analytics for a date range. *Accept:* KPIs computed by SQL aggregation; empty range → zeros, not errors.
- **US-8** As faculty/admin, I run the optimization simulator. *Accept:* returns ranked, explainable alternatives; creating a booking re-validates through the booking service.

## 4. Functional requirements
FR-1 Issue CRUD + workflow + activity history. FR-2 Explainable priority recommendation. FR-3 Resource catalogue with filters. FR-4 Booking lifecycle (request/approve/reject/cancel) + conflict alternatives. FR-5 Concurrency-safe booking (pessimistic resource-row lock). FR-6 Analytics KPIs + category/resource rankings. FR-7 Optimization recommendations with scoring explanation. FR-8 Session auth, BCrypt, CSRF, role + object-level authorization.

## 5. Non-functional requirements
NFR-1 Layered MVC; DTOs only at API boundary. NFR-2 Flyway-managed schema, no `ddl-auto=update` in prod. NFR-3 Paginated lists, DB-side aggregation. NFR-4 Consistent error envelope; no stack traces/SQL leakage. NFR-5 Timestamps stored UTC (`Instant`), displayed in campus zone (default `Asia/Kolkata`, configurable). NFR-6 Tests: unit + integration + security; concurrency test for bookings.

## 6. Scope exclusions (MVP)
File-upload storage (metadata only), email/SMS notifications, real AI/ML prioritisation, microservices/Kafka/Redis/Kubernetes, multi-campus tenancy.
