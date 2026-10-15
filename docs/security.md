# CampusOS — Security

## Authentication
Server-side sessions, BCrypt (`BCryptPasswordEncoder`), HttpOnly + SameSite=Lax session cookie, CSRF token cookie (`XSRF-TOKEN`, JS-readable) sent back as `X-XSRF-TOKEN` header. No tokens in localStorage. Logout invalidates session server-side.

## Authorization (defence in depth)
1. URL rules in `SecurityConfig` (authenticated by default, login/public assets permitAll).
2. Method rules (`@PreAuthorize` / service checks) for role gates (assign, approve, admin).
3. Object-level checks in services (issue visibility, booking ownership) — hiding buttons is never the control.

## Threats & mitigations
- **IDOR**: every detail/mutation re-checks visibility/ownership; invisible issue → 404 to avoid enumeration (403 where role context matters).
- **Mass assignment**: request DTOs whitelist fields; server controls status/reference/timestamps; `PATCH` resource rejects role/status injection.
- **Injection**: JPA parameterised queries only; sort fields allow-listed.
- **XSS (frontend)**: textContent-based rendering; no untrusted innerHTML.
- **Credential leakage**: error envelope never includes SQL/stack/password data; secrets via env vars (`DB_*`, `SESSION_SECRET` if used).
- **CSRF**: enabled for cookie auth; login CSRF handled by Spring's token repo.

## Secrets & audit
`.env.example` documents vars; `.gitignore` excludes `.env`. Demo seeder only under `local` profile with clearly-labelled demo passwords (documented in README as local-only). IssueActivity + booking status changes provide the audit trail; admin actions recorded as activities where applicable.
