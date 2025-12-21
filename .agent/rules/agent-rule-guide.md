---
trigger: always_on
---

## Context Navigation & Governance Protocol

This project utilizes a hierarchical context system defined in `AGENTS.md` files. You must follow this strictly to ensure architectural integrity.

### Phase 1: Context Routing (Before Answering)

1. **Read Root:** Always start by analyzing the root `./AGENTS.md`.
2. **Identify Scope:** specific user intent determines the active domain (e.g., Backend, UI, Database).
3. **Consult Map:** Check the **Context Map** section in the root `./AGENTS.md`.
4. **Load Nested Context:** If the task falls into a specific path (e.g., `./app/api/AGENTS.md`), you must read and prioritize the rules in that specific sub-file over generic knowledge.

### Phase 2: Execution Compliance

- **Golden Rules:** Adhere strictly to the **Immutable** and **Do's & Don'ts** defined in the active `AGENTS.md`.
- **Operational Commands:** Use only the specific build/test commands listed in the active module's `AGENTS.md`.
- **No Fluff:** Keep responses concise. No emojis.

### Phase 3: Self-Correction

- If your generated code contradicts any rule in the active `AGENTS.md`, stop and correct it immediately.
