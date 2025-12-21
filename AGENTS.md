# Role

**AI Context & Governance Architect** for IAM Project (iam-mvp).

# Project Context & Operations

- **Domain:** Identity & Access Management (IAM)
- **Tech Stack:** Java 21, Spring Boot 3.5.8, Gradle, PostgreSQL, RabbitMQ, Docker
- **Structure:** Multi-module Gradle project (Core, Connectors)
- **Operational Commands:**
  - Build: `./gradlew build`
  - Run (All): `docker-compose up -d`
  - Run (Core): `./gradlew :iam-core:bootRun`
  - Test: `./gradlew test`

# Golden Rules

- **Immutable:**
  - Strictly follow Java 21 syntax (Records, Pattern Matching).
  - Ensure 12-Factor App principles for containerization.
  - Never modify `AGENTS_md_Master_Prompt_ghs8S.md`.

- **Do's:**
  - Use `Lombok` for boilerplate reduction (`@RequiredArgsConstructor`, `@Getter`).
  - Use Constructor Injection over Field Injection.
  - Use `./gradlew` for all build tasks.
  - Keep modules decoupled; `iam-core` should not depend on connectors.

- **Don'ts:**
  - Do not use `@Autowired` on fields.
  - Do not hardcode secrets (DB passwords, API keys) in code; use environment variables.
  - Do not use System.out.println; use SLF4J logging.

# Standards & References

- **Code Style:** Standard Java/Spring Boot conventions.
- **Git:** Semantic Commit Messages (`feat:`, `fix:`, `chore:`).
- **Functional Specification:** [spec.mb](./spec.mb) — Source of truth for domain logic and data schemas.
- **Maintenance Policy:** Suggest updates to this file if patterns change.

# Context Map

- **[Core Logic (BE)](./iam-core/AGENTS.md)** — Core Identity entities, services, and business logic.
- **[AD Connector (BE)](./iam-connector-ad/AGENTS.md)** — Active Directory integration, provisioning, and synchronization.
- **[HR Connector (BE)](./iam-connector-hr/AGENTS.md)** — HR System source integration and events.
- **[Infrastructure (Ops)](./docker-compose.yml)** — Docker services for Postgres, RabbitMQ, and Apps.
- **[Build Settings (Root)](./build.gradle)** — Root project configuration and dependencies.
