# Role

**AI Context & Governance Architect** for IAM Project (iam-mvp).

# Project Context & Operations

- **Domain:** Identity & Access Management (IAM) - SCIM 2.0 Compliant
- **Tech Stack:** Java 21, Spring Boot 3.4.3, Spring Cloud 2024.0.0, Gradle, PostgreSQL 17, RabbitMQ, Docker
- **Structure:** Multi-module Gradle project. Active modules (per `settings.gradle`): `iam-eureka`, `iam-registry`, `iam-engine`, `iam-adapter-db`. The pre-MSA `iam-core/` source directory is retained for reference only — not in `settings.gradle`, not in `docker-compose.yml`.
- **Operational Commands:**
  - **Hybrid Development (Highly Recommended):**
    - Start Infra: `docker-compose -f docker-compose.infra.yml up -d`
    - Run Apps: Use IDE (IntelliJ/VSCode) for debugging and rapid development.
  - **Full Stack (Containerized):**
    - `docker-compose up -d`
    - Port map: Eureka 8761, iam-registry 18081, iam-engine 18085, iam-adapter-db 18086, iam-ui 15173.
  - Build: `./gradlew build`
  - Run (Registry): `./gradlew :iam-registry:bootRun`
  - Test: `./gradlew test`

# Golden Rules

- **Immutable:**
  - Strictly follow Java 21 syntax (Records, Pattern Matching for Switch on `ExtensionData`).
  - Ensure 12-Factor App principles for containerization.

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
  - Do not use hardcoded strings for attribute names, event types, or status codes. Use `AttributeConstants` or `SyncConstants` instead.

# Standards & References

- **Code Style:** Standard Java/Spring Boot conventions.
- **Git:** Semantic Commit Messages (`feat:`, `fix:`, `chore:`).
- **API Specification:** [spec.md](./spec.md) — API specs have been consolidated into the master spec document.
- **Maintenance Policy:** Suggest updates to this file if patterns change.

# Context Map

- **[Master Specification](./spec.md)** — Source of truth for all domain logic, architecture, and API endpoints.

**Backend Microservices (MSA)**

- **[Registry Service](./iam-registry/AGENTS.md)** — Core Identity Storage & SCIM API Provider.
- **[Transformation Engine](./iam-engine/AGENTS.md)** — Groovy Rule Engine & Core logic orchestration.
- **[Database Adapter](./iam-adapter-db/AGENTS.md)** — Connectivity Adapter for Dynamic DB Fetch/Update.
- **[Service Discovery](./iam-eureka/AGENTS.md)** — Eureka Server configurations.

**Retired (reference only)**

- **[Core Logic (Retired)](./iam-core/AGENTS.md)** — Pre-MSA monolith precursor. Removed from `settings.gradle` and `docker-compose.yml` so it no longer builds or runs. Source directory kept for historical reference. Do **not** add code here.

**Frontend & Infra**

- **[UI Dashboard](./iam-ui/AGENTS.md)** — High-density Vue 3 dashboard and Miller Columns.
- **[Infrastructure (infra-only)](./docker-compose.infra.yml)** — Postgres + RabbitMQ for hybrid development.
- **[Infrastructure (full stack)](./docker-compose.yml)** — Active modules + Postgres + RabbitMQ.
- **[Build Settings](./build.gradle)** — Root config. Active modules declared in [`settings.gradle`](./settings.gradle).

**Claude Code Native Entry Point**

- **[CLAUDE.md](./CLAUDE.md)** — Root context auto-loaded by Claude Code. Per-module `CLAUDE.md` files are pointer-shims to the detailed `AGENTS.md` in each directory.

## Living Documentation & Feedback Loop

- **Spec Synchronization:**
  - After completing any feature development or refactoring, you MUST update `spec.md` to reflect the actual implementation.
  - Ensure that Class names, Database schemas (DDL), and JSON payload examples in `spec.md` are 100% consistent with the final code.
  - If any architectural decisions or edge cases were discovered during coding, document them in the "Implementation Notes" section of `spec.md`.

- **Context Preservation:**
  - Treat `spec.md` as the "Single Source of Truth" for the AI's mental map.
  - When starting a new task, always cross-reference the updated `spec.md` to ensure continuity and prevent regression.
