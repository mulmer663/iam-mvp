# Module Context

**Core Identity Engine:** Central hub for identity lifecycle management, policy enforcement, and persistence.
**Dependencies:** `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-amqp`

# Tech Stack & Constraints

- **Database:** PostgreSQL (via `spring-data-jpa`).
- **Messaging:** RabbitMQ (Events for provisioning).
- **Validation:** `javax.validation` / `hibernate-validator`.
- **Constraint:** No direct dependency on `iam-connector-*` modules.

# Implementation Patterns

- **Entity Design:** Use JPA Entities with `Lombok`. Relations should be Lazy by default.
  - `IamUser`: UUID (PK), loginId (unique), name, status.
  - `IamUserExtension`: userId (PK/FK), attributes (jsonb).
  - `IdentityLink`: systemType, externalId (Index), iamUserId.
- **Repository:** `JpaRepository` interface based.
- **Service Layer:** Business logic here. `@Transactional` usually applies at this layer.
- **Event Publication:** Publish generic events (e.g., `UserCreatedEvent`) to RabbitMQ for connectors.

# Testing Strategy

- **Unit:** `Mockito` for Service logic.
- **Integration:** `@DataJpaTest` for repositories.
- **Command:** `./gradlew :iam-core:test`

# Local Golden Rules

- **Do's:**
  - Use DTOs for API/Event payloads; never expose `@Entity` directly.
  - Use `Optional<T>` for return types in Services/Repositories where applicable.
  - **Logic Flow (processHrSync):** Check `IdentityLink` -> Create new `IamUser`/`Extension`/`Link` (New) or Update existing (Update) -> Emit provisioning command.
- **Don'ts:**
  - Do not put specific connector logic (e.g., "AD Attribute Mapping") here.
  - Do not ignore `LazyInitializationException` warnings.
