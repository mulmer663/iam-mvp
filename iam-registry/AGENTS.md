# Role

**Identity Registry Architect** for `iam-registry` module.

# Context & Domain

- **Domain:** Core Identity Storage & SCIM API Provider
- **Tech Stack:** Java 21, Spring Boot 3.4.x, Spring Data JPA, Hibernate Envers, Hypersistence Utilities, PostgreSQL
- **Responsibility:** Act as the authoritative source of truth (Golden Record) for identities. Provides REST APIs for external consumption and internal system configuration. Consumes `CDM_DATA_QUEUE` to persist transformed identities.

# Golden Rules

- **Immutable:**
  - **Traceability:** Every modification to `IamUser` or `SyncHistory` MUST retain the `traceId` to link to the original event.
  - **Identities:** `IamUser` entity MUST use Hypersistence `TSID` as the primary key.
  - **Security:** Use proper JPA configuration and DB connection pooling.
- **Do's:**
  - Keep entities decoupled from the Transformation Engine logic.
  - Use Envers `@Audited` for complete snapshot retention on `IamUser` and extensions.
  - Handle SCIM 2.0 representations (`/scim/v2/Users`, `/scim/v2/Schemas`) within the `@RestController` layers.
- **Don'ts:**
  - Do not process external raw data here. The Engine does the transformation.
  - Do not send direct queries to external target databases (use RabbitMQ outward flowing to Adapter DB).

# SCIM & API Implementation

All REST APIs here must conform to the Master Technical Document (`spec.md`), returning the standard SCIM representations for error handling and resources.

# Context Map

- **[Master AGENTS.md](../AGENTS.md)** — Root project governance and navigation.
