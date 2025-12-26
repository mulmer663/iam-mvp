# Role

**AI Context & Governance Architect** for IAM Project (iam-mvp).

# Project Context & Operations

- **Domain:** Identity & Access Management (IAM) - SCIM 2.0 Compliant
- **Tech Stack:** Java 21, Spring Boot 3.5.8, Gradle, PostgreSQL, RabbitMQ, Docker
- **Structure:** Multi-module Gradle project (Core, Connectors)
- **Operational Commands:**
  - Build: `./gradlew build`
  - Run (All): `docker-compose up -d`
  - Run (Core): `./gradlew :iam-core:bootRun`
  - Test: `./gradlew test`

# Golden Rules

- **Immutable:**
  - Strictly follow Java 21 syntax (Records, Pattern Matching for Switch on `ExtensionData`).
  - Ensure 12-Factor App principles for containerization.
  - Never modify `AGENTS_md_Master_Prompt_ghs8S.md`.

- **Do's:**
  - Use `Lombok` for boilerplate reduction (`@RequiredArgsConstructor`, `@Getter`).
  - Use Constructor Injection over Field Injection.
  - Use `./gradlew` for all build tasks.
  - Keep modules decoupled; `iam-core` should not depend on connectors.
  - **Consistency & Reusability (UI):**
    - Enforce uniform UX across all modules. If a list view is used in one module, use the same component and design tokens for similar lists in other modules (e.g., embedded history lists).
    - Reuse components (like `SyncHistory` logic) rather than duplicating code.
    - Ensure visual hierarchy is consistent (Level 1 -> Level 2 -> Level 3 -> Level 4).
  - **Data Linkage (Traceability):**
    - Data flow MUST be traceable: `HR -> IAM Core -> Prov System`.
    - Every event must be linked via a unique `traceId` (or correlation ID) to its upstream and downstream counterparts.
    - Change Logs must explicitly link to the `Source Sync` event that triggered them.
    - Integration Syncs must explicitly link to the `Source Sync` or `Change Log` event that triggered them.

- **Don'ts:**
  - Do not use `@Autowired` on fields.
  - Do not hardcode secrets (DB passwords, API keys) in code; use environment variables.
  - Do not use System.out.println; use SLF4J logging.

# Standards & References

- **Code Style:** Standard Java/Spring Boot conventions.
- **Git:** Semantic Commit Messages (`feat:`, `fix:`, `chore:`).
- **Functional Specification:** [spec.md](./spec.md) — Source of truth for domain logic and data schemas.
- **Maintenance Policy:** Suggest updates to this file if patterns change.

# Context Map

- **[Core Logic (BE)](./iam-core/AGENTS.md)** — Core Identity entities, services, and business logic.
- **[AD Connector (BE)](./iam-connector-ad/AGENTS.md)** — Active Directory integration, provisioning, and synchronization.
- **[HR Connector (BE)](./iam-connector-hr/AGENTS.md)** — HR System source integration and events.
- **[UI Dashboard (FE)](./iam-ui/AGENTS.md)** — High-density Vue 3 dashboard and Miller Columns.
- **[Infrastructure (Ops)](./docker-compose.yml)** — Docker services for Postgres, RabbitMQ, and Apps.
- **[Build Settings (Root)](./build.gradle)** — Root project configuration and dependencies.
