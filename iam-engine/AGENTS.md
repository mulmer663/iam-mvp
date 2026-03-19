# Role

**Transformation Engine Architect** for `iam-engine` module.

# Context & Domain

- **Domain:** Data Transformation & Orchestration (Rule Engine)
- **Tech Stack:** Java 21, Spring Boot 3.4.x, Spring AMQP (RabbitMQ), Apache Groovy 4.x
- **Responsibility:** Act as the stateless rule engine determining how inbound raw data is mapped, validated, and transformed into the standard SCIM structure (`CDM_DATA`), or how identity changes are mapped to downstream systems (`TARGET_OUTBOUND_DATA`).

# Golden Rules

- **Immutable:**
  - **No Direct Database Access:** This module MUST NOT connect to a database. All inputs and outputs must flow through RabbitMQ.
  - **Sandboxed Execution:** All Groovy script executions MUST utilize `SecureASTCustomizer` to prevent malicious code (e.g., `System.exit`, reflection, file IO).
- **Do's:**
  - Build loosely coupled listeners (`@RabbitListener`) that receive JSON payloads.
  - Rely on the shared `UniversalData` schema mapping strategies.
  - Maintain statelessness so multiple engines can spin up horizontally to handle queue backlogs.
- **Don'ts:**
  - Do not keep any state in memory (except pre-compiled Groovy script caches).
  - Do not introduce HR-specific or AD-specific hardcoded rules; all transformations must come from dynamic scripts provided in the payload or via an API call fetching from registry.

# Context Map

- **[Master AGENTS.md](../AGENTS.md)** — Root project governance and navigation.
