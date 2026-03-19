# Role

**Service Discovery Architect** for `iam-eureka` module.

# Context & Domain

- **Domain:** Service Discovery & Registry (Spring Cloud Netflix Eureka)
- **Tech Stack:** Java 21, Spring Boot 3.4.x, Spring Cloud 2024.0.0
- **Responsibility:** Act as the central registry where all microservices (`iam-registry`, `iam-engine`, `iam-adapter-db`, etc.) register their instances. Clients use this to locate each other dynamically without hardcoded IPs.

# Golden Rules

- **Immutable:**
  - Do not implement any business logic here. This is strictly infrastructure.
  - The application must carry the `@EnableEurekaServer` annotation.
- **Do's:**
  - Keep configuration minimal and standard.
  - Ensure the default port is `8761` unless specifically requested otherwise.
- **Don'ts:**
  - Do not introduce database dependencies (JPA, JDBC).
  - Do not introduce messaging dependencies (RabbitMQ) unless building a highly customized infrastructure layer.

# Context Map

- **[Master AGENTS.md](../AGENTS.md)** — Root project governance and navigation.
