---
trigger: glob
globs: *.java
---

# Java 21 & Spring Clean Architecture Guide

## 1. Spring Clean Architecture

* **Layer Separation:**
  * `Domain`: Core entities and business logic (Pure Java, no dependencies).
  * `Application`: Use cases, input/output ports, and services.
  * `Infrastructure`: Persistence (DB), external API adapters, and configurations.
  * `Interface/Web`: REST Controllers and DTO mapping.
* **Dependency Rule:** Dependencies must point inwards. The Domain layer must never depend on outer layers.
* **DIP:** Use interfaces to decouple high-level logic from low-level implementation details.

## 2. Java 21 Modernization

* **Data Structures:** Use `record` for DTOs and VOs. Use `List.of()` or `.toList()` for immutable collections.
* **Control Flow:** Use **Pattern Matching** for `instanceof` and `switch` expressions with record patterns.
* **Concurrency:** Use **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) for I/O-bound tasks.
* **Modern Syntax:** Use `var` for local variables, **Text Blocks** (`"""`) for multi-line strings, and `getFirst()`/`getLast()` for sequenced collections.

## 3. Security & Hardening

* **Validation:** Strictly validate all input at the entry point using `jakarta.validation`.
* **Injection Prevention:** Use `PreparedStatement` for SQL and `ProcessBuilder` for OS commands. Use `SecureRandom` for sensitive tokens.
* **Data Privacy:** Never log PII. Use `char[]` for passwords. Avoid Java Serialization; use Jackson/JSON for data exchange.
