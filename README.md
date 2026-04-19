# IAM MVP — 모놀리식 IAM을 MSA로 재설계한 토이 프로젝트

## 왜 만들었나

3년간 엔터프라이즈 IAM/EAM 솔루션을 개발하면서 구조적으로 아쉬운 부분들이 있었습니다.
HR → IAM → Target으로 흐르는 데이터 파이프라인이 단일 서버 안에 강하게 결합되어 있고,
변환 로직이 코드에 하드코딩되어 있어 고객사마다 커스터마이징 비용이 높았습니다.

"내가 처음부터 설계한다면 어떻게 할까?"라는 질문에서 시작한 프로젝트입니다.
회사 도메인과 코드를 공유하지 않으며, 동일한 문제를 다른 방식으로 풀어본 개인 실험입니다.

## 핵심 설계 방향

### 1. 모놀리식 → MSA (Event Choreography)
기존 구조는 HR 연동, 변환, 저장, 프로비저닝이 단일 서버 안에서 동기적으로 처리됩니다.
이 프로젝트는 각 역할을 독립 서비스로 분리하고 RabbitMQ 이벤트로 연결했습니다.

- `iam-adapter-db` : 외부 DB 연결만 담당. 비즈니스 로직 없음
- `iam-engine` : Groovy 기반 런타임 변환 엔진
- `iam-registry` : SCIM 2.0 규격 준수 Identity 저장소
- `iam-eureka` : 서비스 디스커버리

### 2. 런타임 변환 룰 엔진
기존 방식은 SQL 기반 컬럼 매핑 설정으로 변환 규칙을 표현했습니다.
설정으로 처리할 수 없는 복잡한 케이스는 결국 코드로 예외 처리해야 했고,
표현할 수 있는 변환의 범위가 SQL 문법에 종속된다는 한계가 있었습니다.

여기서는 변환 규칙을 DB에 저장하고 Groovy 스크립트로 런타임에 실행합니다.
SecureASTCustomizer로 샌드박싱하여 임의 코드 실행을 차단합니다.

지원 변환 패턴: DIRECT(1:1 매핑) / CODE(코드 치환) / CLASSIFY / REPLACE / CUSTOM(Groovy snippet)

### 3. SCIM 2.0 표준 구현
독점적인 API 구조 대신 SCIM 2.0 표준을 직접 구현했습니다.
스키마, ResourceType, 동적 엔드포인트까지 런타임에 등록 가능한 구조입니다.

### 4. Core/Extension 하이브리드 스토리지
- Core 속성 (userName, active 등): 정형 컬럼으로 관리 (검색/필터 최적화)
- Extension 속성: PostgreSQL JSONB로 저장 (스키마 변경 없이 확장 가능)

### 5. traceId 기반 이력 추적
Hibernate Envers + CustomRevisionEntity로 모든 변경을 스냅샷으로 보관합니다.
traceId를 통해 특정 변환 파이프라인 시점의 전체 상태를 복원할 수 있습니다.

## 기술 스택

- Java 21, Spring Boot 3, Spring Cloud (Eureka)
- RabbitMQ (Event Choreography)
- PostgreSQL (JSONB), Hibernate Envers
- Groovy 4.x (Rule Engine), SecureASTCustomizer
- SCIM 2.0, Docker Compose

## 현재 구현 상태

- [x] MSA 모듈 분리 및 서비스 디스커버리
- [x] RabbitMQ 기반 이벤트 파이프라인
- [x] Groovy 런타임 룰 엔진 (샌드박싱 포함)
- [x] SCIM 2.0 표준 API (Users, Schemas, ResourceTypes)
- [x] Core/Extension 하이브리드 스토리지
- [x] Hibernate Envers 기반 이력 추적
- [ ] 프로비저닝 아웃바운드 파이프라인 (진행중)
- [ ] UI 연동 완성

## 로컬 실행

docker-compose up -d
