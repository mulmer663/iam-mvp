# Role

**Source Event Detector** for HR System Integration.

# Module Context

- **HR Connector:** Detects changes in the HR system (Upstream) and pushes **Raw Snapshots** to Core.
- **Responsibilities:** Change detection, Raw data extraction, and Initial `traceId` generation.
- **Dependencies:** `spring-boot-starter-web`, `spring-boot-starter-amqp`

# Tech Stack & Constraints

- **Source:** HTTP REST API (Mock/Real HR System).
- **Messaging:** RabbitMQ (Producer).
- **Constraint:** Read-only access to HR System.

# Implementation Patterns

- **Batch Ingestion (Fingerprint Strategy):**
  - **Full Pull:** Fetch the entire dataset from the HR system during each batch cycle (Daily/Manual).
  - **Change Detection:** Calculate a **SHA-256 hash** of the raw HR record.
  - **Minimal Local State:** Store only `externalId`, `hashValue`, and `lastSeen` in a lightweight local DB (e.g., SQLite or Redis) to identify ADD, UPDATE, or DELETE actions.
  - **Reconciliation:** After each batch, any `externalId` in the local state not found in the latest HR pull must be treated as a DELETE/TERMINATION event.

# Local Golden Rules

- **Do's:**
  - Generate a **`traceId`** for the entire batch job and include it in every individual event sent to Core.
  - Send the **Raw Payload** exactly as received from HR; do not filter or transform attributes.
  - Perform the hash calculation on the "canonicalized" JSON (sorted keys) to prevent false positives from key reordering.
- **Don'ts:**
  - Do not keep the hash of PII (Personally Identifiable Information) if sensitive; the hash itself is a fingerprint, but ensure the storage is secure.

# Testing Strategy

- **Unit:** `RestClientTest` for HR API consumption.
- **Integration:** Verify AMQP message publishing to the `iam.ingest` exchange.
- **Command:** `./gradlew :iam-connector-hr:test`
