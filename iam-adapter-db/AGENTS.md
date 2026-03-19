# Role

**Connectivity Adapter Developer** for `iam-adapter-db` module.

# Context & Domain

- **Domain:** Pure Database Connectivity (JDBC) and Message Broadcasting
- **Tech Stack:** Java 21, Spring Boot 3.4.x, Spring JDBC, Spring AMQP (RabbitMQ)
- **Responsibility:** Act as a "dumb" pipe that fetches data dynamically from an external data source and publishes it exactly as-is to RabbitMQ (`RAW_INBOUND_DATA`), or consumes messages from RabbitMQ (`TARGET_OUTBOUND_DATA`) to write dynamically to a target database.

# Golden Rules

- **Immutable:**
  - **No Business Logic & No Transformation:** Never write mapping rules, domain logic, or SCIM transformations here. This module knows absolutely nothing about `IamUser` or target schemas.
  - This module must support dynamic connections based on payload parameters (or UI configurations) rather than hardcoded `application.yml` datasources if possible.
- **Do's:**
  - Use `JdbcTemplate` with dynamic `DriverManagerDataSource` or connection pooling per source.
  - Convert `ResultSet` to basic `Map<String, Object>` or JSON structures before publishing.
  - Rely on RabbitMQ for all inter-service communication.
- **Don'ts:**
  - Do not import IAM core domain annotations or JPA entities here.
  - Do not hardcode domain-specific queries containing logic; queries should be provided via configuration or dynamic input.

# Architecture Traceability

- **Inbound Flow:** `Fetch from DB/API` -> `Produce to [RAW_INBOUND_DATA]`
- **Outbound Flow:** `Consume from [TARGET_OUTBOUND_DATA]` -> `Execute SQL/API on Target DB`

# Context Map

- **[Master AGENTS.md](../AGENTS.md)** — Root project governance and navigation.
