# Module Context

**AD Connector:** Downstream provisioning agent for Microsoft Active Directory.
**Dependencies:** `spring-boot-starter-data-ldap`, `spring-boot-starter-amqp`

# Tech Stack & Constraints

- **Target:** Active Directory / LDAP.
- **Messaging:** RabbitMQ (Consumer of Core events).
- **Constraint:** Must handle "User Already Exists" and network failures gracefully.

# Implementation Patterns

- **Listener:** `RabbitListener` consumes `UserProvisioningCommand`.
- **Payload (CREATE_ACCOUNT):** `targetSystemId` (AD ID), `attributes` (cn, department, description).
- **Mapping:** Map Core specific attributes to AD LDAP attributes (`sAMAccountName`, `userPrincipalName`).
- **Repository:** `LdapRepository` or `LdapTemplate`.

# Testing Strategy

- **Unit:** Mock `LdapTemplate`.
- **Command:** `./gradlew :iam-connector-ad:test`

# Local Golden Rules

- **Do's:**
  - Ensure operations are Idempotent (safe to retry).
  - Log specific LDAP error codes for debugging.
- **Don'ts:**
  - Do not modify Core database directly.
  - Do not block the queue listener indefinitely; use Dead Letter Queues (DLQ) for failures.
