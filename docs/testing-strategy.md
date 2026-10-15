# CampusOS — Testing Strategy

## Levels
- **Unit (JUnit 5 + Mockito)**: priority scoring bands/explanations, workflow transition table (valid + invalid), overlap predicate incl. adjacency, optimization scoring order, service exception paths.
- **Integration (`@SpringBootTest` + MockMvc, H2 MySQL-mode, Flyway)**: auth (401/403), issue visibility (student cannot read other's issue), assign role gate, booking conflict (sequential + concurrent two-thread test asserting exactly one success), approval flow, pagination/filter, analytics on seeded fixtures.
- **Security**: unauthenticated → 401; student assign → 403; crafted privileged fields ignored.

## MySQL fidelity note
Tests run on H2 in MySQL-compatibility mode (Docker daemon unavailable in this environment; Testcontainers needs Docker). Pessimistic-lock semantics (`FOR UPDATE`) and overlap SQL are exercised on H2; before production, re-run the booking concurrency test against MySQL 8 via `docker compose up` + Testcontainers profile. This limitation is explicit, not hidden.

## Running
```
cd backend && ./mvnw test          # all tests (H2, no Docker needed)
./mvnw test -Dtest=BookingConcurrencyTest   # concurrency proof
```
Frontend: manual browser verification against seeded backend (login per role, create issue, book, approve, analytics) + `node --check` on JS modules. No frontend test framework (vanilla constraint).

## Test data
Deterministic fixtures built in-test (users per role, 2 labs, overlapping windows). Seeded demo data is separate (`local` profile) and labelled demonstration data in UI/README.
