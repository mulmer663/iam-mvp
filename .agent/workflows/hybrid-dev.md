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

3. (Note) You can now start `iam-core`, `iam-connector-ad`, or `iam-connector-hr` in your IDE. They are pre-configured to connect to `localhost:5432` and `localhost:5672`.

4. (Note) If you still want to run apps in Docker, use `docker-compose up -d`. They are mapped to `18081`, `18082`, etc., to avoid port conflicts with your IDE.
