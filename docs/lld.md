# CampusOS — Low-Level Design

## 1. Domain model
- **User**(id, email unique, passwordHash, displayName, role, department, active, version)
- **Issue**(id, reference unique `ISS-####`, title, description, category, priority, status, building, locationDetail, reporter→User, assignee→User nullable, createdAt, updatedAt, resolvedAt, resolutionNotes, version `@Version`)
- **IssueActivity**(id, issue→Issue, actor→User, action, fromStatus, toStatus, note, createdAt) — append-only audit.
- **Resource**(id, name, type, building, location, capacity, description, status `ACTIVE/MAINTENANCE/INACTIVE`, requiresApproval, equipmentTags CSV, version)
- **Booking**(id, resource→Resource, owner→User, purpose, startAt, endAt, status `PENDING/CONFIRMED/REJECTED/CANCELLED`, cancelReason, createdAt, version)

## 2. Issue workflow (single source of truth: `IssueWorkflow`)
```
REPORTED → TRIAGED, CANCELLED
TRIAGED → ASSIGNED, CANCELLED, REJECTED
ASSIGNED → IN_PROGRESS, TRIAGED (unassign), CANCELLED
IN_PROGRESS → RESOLVED, ASSIGNED (pause back)
RESOLVED → CLOSED, IN_PROGRESS (reopen)
CLOSED → IN_PROGRESS (reopen, reporter/admin only, within policy)
REJECTED/CANCELLED → terminal
```
Invariants: RESOLVED requires resolution notes; assignment requires technician role; every transition writes IssueActivity in the same transaction (failure ⇒ no partial audit). Arbitrary `status` in PATCH is ignored/rejected — transitions only via `POST /issues/{id}/transitions`.

## 3. Priority service (`IssuePriorityService`)
Deterministic score 0–100: safety category +30, CRITICAL base, affected-people bands, operational impact, waiting-time ageing (+2/day, cap +20). Maps to LOW/MEDIUM/HIGH/CRITICAL bands and returns `PriorityRecommendation(priority, score, explanation)`. Pure function — unit-tested.

## 4. Booking concurrency strategy
`BookingService.create` runs in one transaction:
1. `resourceRepository.findByIdForUpdate(id)` — pessimistic write lock on the resource row.
2. Validate window (start<end, not in past beyond 5-min grace).
3. Overlap query: active statuses (PENDING if resource blocks, CONFIRMED) where `start < :end AND end > :start`; adjacent allowed (strict inequalities).
4. Conflict ⇒ `BookingConflictException` (409) carrying up to 3 alternative slots (next free windows) — never inserts.
5. Insert booking; commit releases the lock.
Two concurrent requests for the same resource serialise on step 1, so both cannot pass step 3. Different resources proceed in parallel. Lock ordering is single-row ⇒ no deadlock. Integration test fires two concurrent overlapping creates and asserts exactly one succeeds.

## 5. Optimization simulator (`OptimizationService`)
Input: desired type/capacity/equipment/time. Candidates: ACTIVE resources matching type, capacity ≥ requested, equipment superset. Each candidate availability-checked; score = capacity fit (30) + equipment fit (25) + free-at-requested-time (25) + same-building bonus (10) + utilisation-balance (10). Returns ranked list with per-factor explanation + utilisation estimate. Labelled as estimate. Booking from a recommendation calls the normal `BookingService` (re-validation + lock).

## 6. Error handling
`@RestControllerAdvice` maps: validation → 400 `VALIDATION_ERROR` + fieldErrors; unauthenticated 401; forbidden 403; not-found 404; conflict/illegal-transition 409; business rule 422 where apt; unexpected 500 (logged, generic message). Envelope: `{timestamp,status,code,message,path,fieldErrors?}`.

## 7. Patterns used (justified)
Strategy-like scoring components (priority, optimization) — interchangeable weights; Specification-style filter predicates for issue search; DTO mappers per module (static, no framework magic). No factories/service-locators — constructor injection throughout.
