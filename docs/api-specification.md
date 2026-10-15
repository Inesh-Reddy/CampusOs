# CampusOS — API Specification

Base: `/api/v1`. Session cookie auth + `X-XSRF-TOKEN` header for mutating requests. All bodies JSON. Lists: `?page=0&size=20&sort=createdAt,desc`.

## Auth
- `POST /auth/login` `{email,password}` → 200 `{id,email,displayName,role,department}`; 401 bad credentials.
- `POST /auth/logout` → 204.
- `GET /auth/me` → current user or 401.

## Issues
- `POST /issues` (auth) `{title,description,category,building,locationDetail,priority?,affectedPeople?,safetyRisk?}` → 201 IssueResponse (includes priorityRecommendation).
- `GET /issues?status=&category=&priority=&q=&mine=&assignedToMe=` → paged IssueResponse (scope-filtered server-side).
- `GET /issues/{id}` → detail (403 if not visible).
- `POST /issues/{id}/assign` (Dept/Campus Admin) `{technicianId}` → 200.
- `POST /issues/{id}/transitions` `{toStatus,note?}` → 200; 409 illegal transition.
- `GET /issues/{id}/activity` → activity list.
- `GET /issues/overdue` (staff) → open issues older than 48h.

IssueResponse: `{id,reference,title,description,category,priority,status,building,locationDetail,reporterName,assigneeName,createdAt,updatedAt,resolvedAt,resolutionNotes}`.

## Resources
- `POST /resources` (admins) → 201. `GET /resources?type=&building=&minCapacity=&q=` → list. `GET /resources/{id}`. `PATCH /resources/{id}` (admins).

## Bookings
- `POST /bookings` `{resourceId,purpose,startAt,endAt}` → 201 (PENDING if approval required else CONFIRMED); 409 `{code:BOOKING_CONFLICT, alternatives:[...]}`.
- `GET /bookings?mine=&resourceId=` → paged. `GET /bookings/{id}`.
- `POST /bookings/{id}/approve|reject|cancel` `{reason?}` → 200; 403/409 on rule violation.
- `GET /resources/{id}/schedule?from=&to=` → bookings in window (for calendar).

## Analytics
- `GET /analytics/overview?from=&to=` → `{totalIssues,openIssues,criticalIssues,overdueIssues,avgResolutionHours,resolvedInRange,utilizationRate,conflictAttempts?}` (conflict attempts tracked in-memory counter + note; core KPIs from SQL).
- `GET /analytics/issues?from=&to=` → by-category, by-status, top buildings.
- `GET /analytics/resources` → most/least utilised resources.
- `POST /analytics/optimization/recommendations` `{resourceType,capacity,equipment[],startAt,endAt,building?}` → ranked `[{resource,score,explanation,utilizationEstimate}]` (estimates labelled).

## Errors
`{timestamp,status,code,message,path,fieldErrors}` — codes: VALIDATION_ERROR 400, UNAUTHENTICATED 401, FORBIDDEN 403, NOT_FOUND 404, BOOKING_CONFLICT / ILLEGAL_TRANSITION 409, BUSINESS_RULE 422, INTERNAL_ERROR 500.
