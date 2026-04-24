# iam-eureka — Claude Code 컨텍스트

**역할:** Spring Cloud Netflix Eureka 서비스 디스커버리. 다른 MSA 모듈이 서로를 동적으로 찾도록 제공.

- 상세 규칙: [AGENTS.md](./AGENTS.md)
- 루트 컨텍스트: [../CLAUDE.md](../CLAUDE.md)

## 이 모듈에서 편집 시 기억할 것

- **비즈니스 로직 금지** — 순수 인프라 모듈
- `@EnableEurekaServer` 필수
- 포트는 **8761** (변경 시 모든 클라이언트의 `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` 영향)
- JPA / RabbitMQ 의존성 추가 금지
