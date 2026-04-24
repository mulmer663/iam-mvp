---
description: Start a hybrid development environment (Infra in Docker, Apps in IDE)
---

# Hybrid Development Workflow

This workflow starts the database and message broker in Docker, allowing you to run and debug application modules in your IDE.

// turbo-all

1. Start Infrastructure:
`docker-compose -f docker-compose.infra.yml up -d`

2. Verify Services:
`docker ps`

3. (Note) You can now start the active MSA modules in your IDE: `iam-eureka`, `iam-registry`, `iam-engine`, `iam-adapter-db`. They are pre-configured to connect to `localhost:5432` (Postgres), `localhost:5672` (RabbitMQ), and `localhost:8761` (Eureka).

4. (Note) `docker-compose up -d` currently references obsolete `iam-connector-ad`/`iam-connector-hr` services and will fail to build. Prefer hybrid mode until the compose file is pruned. When fixed, apps map to `18081`+ to avoid IDE port conflicts.

5. (Note) `iam-core` is the legacy pre-MSA monolith and does not need to be run in current workflows.
