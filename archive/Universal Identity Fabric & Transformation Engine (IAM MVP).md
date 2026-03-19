본 아키텍처는 데이터의 원천(Source)과 목적지(Target)에 상관없이, 중간에 표준 데이터 모델(CDM)을 두고 **Groovy 기반의 변환 코어**를 통해 유연하게 신원을 동기화하는 것을 목표로 합니다.

## 1. 아키텍처 개요 (High-Level View)

이 시스템은 데이터 흐름에 따라 **Inbound**와 **Outbound**로 나뉘며, 모든 변환은 단일화된 **Transformation Engine**을 통과합니다.

- **Inbound:** 외부 소스 → Connector → **[Transform to CDM]** → Registry
    
- **Outbound:** Registry → **[Transform to Target]** → Connector → 외부 시스템
    

---

## 2. 모듈별 역할 및 정의 (Spring Cloud 기반)

|**모듈명**|**주요 기술 및 제약**|**핵심 역할**|
|---|---|---|
|**Identity Registry**|`Spring Data JPA`, `PostgreSQL`|최종 신원 데이터(Golden Record) 저장. TSID(PK) 및 JSONB 기반 확장 속성 관리.|
|**Transformation Engine**|`Groovy 4.x`, `SecureASTCustomizer`|**시스템의 심장.** 방향성(In/Out)에 맞는 스크립트를 로드하여 데이터 매핑 수행.|
|**Connectivity Adapter**|`RabbitMQ`, `iam-connector-*`|외부 시스템(AD, DB, API)과의 물리적 프로토콜 통신 담당.|
|**Gateway & Config**|`Spring Cloud Gateway`, `Config`|진입점 관리 및 변환 규칙(Groovy) 설정을 포함한 중앙 집중형 설정 관리.|

### Spring Cloud 인프라 모듈 (Support System)

- **Spring Cloud Gateway:** 모든 외부 요청(API 호출 등)의 진입점입니다. 인증, 인가 및 속도 제한(Rate Limiting)을 처리합니다.
    
- **Spring Cloud Eureka (Service Discovery):** 위 서비스들이 서로의 위치를 동적으로 찾을 수 있게 해주는 전화번호부 역할을 합니다.
    
- **Spring Cloud Config:** 변환에 필요한 Groovy 스크립트 설정이나 DB 접속 정보 등을 중앙에서 관리합니다.
    
- **Spring Cloud Bus / Stream:** RabbitMQ를 통해 데이터 변경 이벤트를 각 모듈에 전파합니다.

---

## 3. 핵심 설계 원칙 (Golden Rules)

### 🧩 데이터 변환 코어 (The Brain)

- **방향성 추상화:** 엔진은 자신이 Inbound를 처리하는지 Outbound를 처리하는지 중요하지 않음. 오직 `Script ID`와 `Input Data`만 받음.
    
- **샌드박스 보안:** `SecureASTCustomizer`를 사용하여 `System`, `Runtime` 등의 위험한 클래스 접근을 원천 차단.
    
- **버전 관리:** 모든 변환 로직은 `IAM_TRANS_RULE_VERSION` 테이블에서 해시(SHA-256)와 함께 버전 관리됨.
    

### 🗄️ 하이브리드 저장소 전략

- **Flattened Columns:** `userName`, `active` 등 공통 필수 속성은 일반 컬럼으로 관리.

- **Structured Extensions:** 각 소스별 가변 데이터는 `IamUserExtension`의 `jsonb` 필드에 저장하여 스키마 변경 최소화.
    

### 📡 이벤트 기반 통신

- **Choreography:** 신원 변경 시 `UserCreatedEvent` 등을 RabbitMQ에 발행하여 연동 서비스에 전파.
    
- **멱등성(Idempotency):** 동일한 이벤트가 여러 번 수신되어도 변환 결과가 일관되도록 설계.
    

---

## 4. 데이터 흐름 상세 (Process Flow)

1. **Normalization (Ingress):** 외부 데이터를 수집하여 표준 JSON 객체로 변환.
    
2. **Mapping (Transform):** * `IdentityLink`를 조회하여 기존 유저 여부 확인.
    
    - Groovy 스크립트를 통해 CDM 또는 타겟 규격으로 매핑 수행.
        
3. **Persistence & Egress:**
    
    - 데이터 저장 시 `IamUser`와 `IamUserExtension` 업데이트.
        
    - 타겟 시스템으로의 전송을 위해 비동기 이벤트 발행.
        

---

## 5. 모니터링 및 가용성 전략

- **스케일 아웃:** 변환 엔진 부하 시 Kubernetes의 HPA를 통해 Pod를 동적으로 증설.
    
- **상태 확인:** `Spring Boot Actuator`의 health 엔드포인트를 K8s Liveness/Readiness Probe에 연동.
    
- **중앙 집중 로깅:** 가벼운 **Grafana Loki**를 활용하여 여러 서버의 변환 로그를 통합 추적.