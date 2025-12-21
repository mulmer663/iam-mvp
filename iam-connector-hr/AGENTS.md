# Module Context

**HR Connector:** Authoritative source ingestion (Upstream). Pushes changes to Core.
**Dependencies:** `spring-boot-starter-web`, `spring-boot-starter-amqp`

# Tech Stack & Constraints

- **Source:** HTTP REST API (Mock/Real HR System).
- **Messaging:** RabbitMQ (Producer).
- **Constraint:** Read-only access to HR System (usually).

# Implementation Patterns

- **Ingestion:** Scheduled Task or Webhook Receiver.
- **Normalization:** Convert raw HR JSON to standardized `UserSyncEvent`.
- **Payload (SYNC_USER):** `hrEmpId` (External ID), `name`, `attributes` (deptCode, position, email).
- **Publisher:** Send events to `iam.hr.exchange`.

# Testing Strategy

- **Unit:** `WebMvcTest` (if webhook) or `RestClientTest`.
- **Command:** `./gradlew :iam-connector-hr:test`

# Local Golden Rules

- **Do's:**
  - Sanitize input data (trim strings, check nulls) before sending to Core.
  - Log the "Raw" payload ID for tracing purposes.
- **Don'ts:**
  - Do not implement complex identity logic (leave that to Core).
  - Do not tightly couple with Core's internal DB schema.
