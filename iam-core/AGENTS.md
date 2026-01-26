# Module Context

**Core Identity Engine:** Central hub for identity lifecycle management, policy enforcement, and persistence.
**Dependencies:** `spring-boot-starter-data-jpa`, `postgresql`, `spring-boot-starter-amqp`, `groovy-all`

## Repository Layout (Clean Architecture)

```text
com.iam.core
├── domain               <-- [Core] Entities, Repositories (Layer > Domain)
│   ├── user             <-- User, IdentityLink, UserRepository
│   ├── scim             <-- ScimSchemaMeta, ScimResourceTypeMeta, IamAttributeMeta, ScimDynamicResource
│   └── common           <-- Shared VOs, Enums, Exceptions
├── application          <-- [Use Cases] Services, DTOs (Layer > Domain)
│   ├── user             <-- UserUpdateService, UserQueryService
│   ├── scim             <-- ScimResourceService, ScimResourceTypeService, ScimDynamicResourceService
│   └── common           <-- Shared DTOs, Base classes
├── adapter              <-- [Infra] Messaging, Web, etc.
│   └── web
│       ├── controller
│       │   ├── user     <-- UserController, UserHistoryController
│       │   └── scim     <-- ScimMetadataController, GenericScimController
│       └── handler      <-- GlobalExceptionHandler, RequestContext
└── config               <-- Spring Config (Security, init, etc.)
```

# Tech Stack & Constraints

- **Database:** PostgreSQL (via `spring-data-jpa`).
- **Messaging:** RabbitMQ (Events for provisioning).
- **Validation:** `javax.validation` / `hibernate-validator`.
- **Script Engine:** Groovy 4.x (with `SecureASTCustomizer` for Sandbox).
- **Constraint:** No direct dependency on `iam-connector-*` modules.

# Implementation Patterns

- **Entity Design:** Use JPA Entities with `Lombok`. Relations should be Lazy by default.
  - **ID Strategy:** Use **TSID (Long)** for numeric primary keys or **UUID (String)** for SCIM standard compatibility as per `spec.md`.
  - `IamUser`: `userName` (unique), `familyName`, `givenName`, `active`, `resourceType`, `created`, `lastModified` (Flattened Columns).
  - `IamUserExtension`: `userId` (PK/FK), `extensions` (jsonb mapping to structured `ExtensionData`).
  - `ScimDynamicResource`: `id` (TSID), `scimId` (Logical ID), `resourceType`, `attributes` (jsonb).
  - `IdentityLink`: systemType, externalId (Index), iamUserId.
- **Repository:** `JpaRepository` interface based.
- **Service Layer:** Business logic here. `@Transactional` usually applies at this layer.
- **Event Publication:** Publish generic events (e.g., `UserCreatedEvent`) to RabbitMQ for connectors.
- **Rule Engine Security:**
  - Use `SecureASTCustomizer` to whitelist allowed classes (`String`, `Math`, `Date`, etc.).
  - **NEVER** allow `System`, `Runtime`, or `ProcessBuilder` in scripts.
  - Scripts must be hashed (SHA-256) and versioned in `IAM_TRANS_RULE_VERSION`.
- **Error Handling & Validation:**
  - **Custom Exception:** Use `IamBusinessException` combined with `ErrorCode` (e.g., `ErrorCode.USER_NOT_FOUND`) for domain-level errors.
  - **Global Handler:** All exceptions are caught by `GlobalExceptionHandler` (`@RestControllerAdvice`) which converts them to a standard JSON error response as defined in [../api-specs.md](../api-specs.md).
  - **Validation:** Use `jakarta.validation` annotations (`@NotNull`, `@Email`, etc.) on Input DTOs. Validation errors are automatically transformed into `400 Bad Request` responses.

# Testing Strategy

- **Unit:** `Mockito` for Service logic.
- **Integration:** `@DataJpaTest` for repositories.
- **Command:** `./gradlew :iam-core:test`

# Local Golden Rules

- **Do's:**
  - Use DTOs for API/Event payloads; never expose `@Entity` directly.
  - Use `Optional<T>` for return types in Services/Repositories where applicable.
  - **Logic Flow (processHrSync):** Normalize incoming SCIM JSON -> Check `IdentityLink` -> Create/Update `IamUser` (Flattened) & `IamUserExtension` (Structured) -> Emit SCIM Command.
- **Don'ts:**
  - Do not put specific connector logic (e.g., "AD Attribute Mapping") here.
  - Do not ignore `LazyInitializationException` warnings.
