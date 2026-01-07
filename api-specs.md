# IAM API Specification (Updated 2026-01-07)

이 문서는 IAM 시스템의 API 명세를 정의합니다. 모든 API는 RESTful 원칙을 따르며, 응답 데이터 포맷은 JSON을 사용합니다.

## 1. 공통 응답 구조 (Error Response)

에러 발생 시 다음과 같은 공통 구조로 응답합니다.

```typescript
interface ErrorResponse {
  errorCode: string;    // 내부 에러 코드 (ex: IAM-4103)
  message: string;      // 사용자 친화적 메시지
  traceId: string;      // 로그 추적용 ID
  timestamp: string;    // 발생 시각
  path: string;         // 요청 경로
  status: number;       // HTTP 상태 코드
}
```

---

## 2. SCIM User API

### 2.1 사용자 상세 조회 (현재 시점)

- **Endpoint**: `GET /scim/v2/Users/{id}`
- **Description**: 특정 사용자의 최신 상세 정보를 SCIM 형식으로 조회합니다.

### 2.2 사용자 상세 이력 조회 (Revision 기반)

- **Endpoint**: `GET /api/v1/history/users/{id}/revisions/{revId}`
- **Description**: Hibernate Envers 리비전 번호를 기반으로 특정 시점의 사용자 SCIM 프로필을 복원합니다.

### 2.3 사용자 상세 이력 조회 (Trace ID 기반)

- **Endpoint**: `GET /api/v1/history/users/{id}/trace_id/{traceId}`
- **Description**: 비즈니스 트랜잭션 ID(`traceId`)를 기반으로 당시의 리비전을 찾아 SCIM 프로필을 복원합니다.
- **Success Response (200 OK)**:

```JSON
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
  ],
  "id": "795604424022992116",
  "externalId": "2023001",
  "userName": "gildong.hong@example.com",
  "name": {
    "familyName": "Hong",
    "givenName": "Gildong",
    "formatted": null
  },
  "title": "Principal Engineer",
  "emails": [
    {
      "value": "gildong.hong@example.com",
      "primary": true
    }
  ],
  "active": true,
  "meta": {
    "resourceType": "User",
    "created": "2026-01-04T19:47:56.302082",
    "lastModified": "2026-01-04T19:47:56.302082",
    "location": "/scim/v2/Users/795604424022992116"
  },
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
    "division": null,
    "costCenter": null,
    "organization": null,
    "department": "DEPT01",
    "employeeNumber": "2023001"
  },
  "urn:ietf:params:scim:schemas:extension:custom:2.0:User": null
}

```

---

## 3. History API

### 3.1 동기화 이력 목록 조회

- **Endpoint**: `GET /api/v1/history`
- **Description**: 전체 동기화 이력 로그를 조회합니다.
- **Query Parameters**: `userId`, `targetUser`, `page`, `size`

```JSON
{
  "content": [
    {
      "id": "795600618894266494",
      "traceId": "T-795600618888508442",
      "eventType": "USER_CREATE",
      "status": "SUCCESS",
      "syncDirection": "RECON",
      "target": "jane.doe",
      "sourceSystem": "SAP_HR",
      "targetSystem": "IAM",
      "time": "2026-01-07T19:32:49.097081",
      "message": "User created via DataInitializer",
      "resultData": {
        "status": "CREATED",
        "syncType": "USER_CREATE"
      },
      "requestPayload": {
        "event": {
          "payload": {
            "email": "jane.doe",
            "empNo": "EXT-101",
            "status": "ACTIVE",
            "lastName": "Doe",
            "firstName": "Jane"
          },
          "traceId": "T-795600618888508442",
          "systemId": "SAP_HR",
          "eventType": "USER_CREATE",
          "timestamp": "2026-01-07T19:32:49.087840"
        }
      },
      "parentHistoryId": null,
      "userRevId": 1,
      "ruleRevId": 1
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "first": true,
  "numberOfElements": 1,
  "empty": false
}
```

### 3.2 사용자 리비전 이력 목록 조회

- **Endpoint**: `GET /api/v1/history/users`
- **Description**: 사용자의 데이터베이스 리비전 기반 변경 이력(Snapshot) 목록을 조회합니다.
- **Query Parameters**:
  - `userId`: IAM User ID (필터)
  - `traceId`: 비즈니스 트랜잭션 ID (필터)
  - `page`: 페이지 번호
  - `size`: 페이지 크기
- **Success Response (200 OK)**:

```JSON
{
  "content": [
    {
      "revId": 4,
      "traceId": "T-796695712788578725",
      "operatorId": "IAM_USER",
      "operationType": "USER_CREATE",
      "timestamp": "2026-01-07T20:04:19.898262",
      "profile": {
        "schemas": [
          "urn:ietf:params:scim:schemas:core:2.0:User",
          "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
        ],
        "id": "796695713097961494",
        "externalId": "EXT-101",
        "userName": "jane.doe",
        "name": {
          "familyName": "Doe",
          "givenName": "Jane",
          "formatted": null
        },
        "title": "External Auditor",
        "emails": [
          {
            "value": "jane.doe",
            "primary": true
          }
        ],
        "active": true,
        "meta": {
          "resourceType": "User",
          "created": "2026-01-07T20:04:19.893376",
          "lastModified": "2026-01-07T20:04:19.893376",
          "location": "/scim/v2/Users/796695713097961494"
        },
        "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User": {
          "division": null,
          "costCenter": null,
          "organization": null,
          "department": "AUDIT-01",
          "employeeNumber": "EXT-101"
        },
        "urn:ietf:params:scim:schemas:extension:custom:2.0:User": null
      }
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "last": true,
  "totalPages": 1,
  "totalElements": 2,
  "first": true,
  "numberOfElements": 2,
  "size": 20,
  "number": 0,
  "sort": {
    "empty": true,
    "sorted": false,
    "unsorted": true
  },
  "empty": false
}
```

---

## 4. Rule Engine Mapping API

### 4.1 필드 매핑 목록 조회 (현재)

- **Endpoint**: `GET /api/v1/rules/{ruleId}/mappings`

### 4.2 필드 매핑 이력 조회 (Revision 기반)

- **Endpoint**: `GET /api/v1/rules/history`
- **Description**: 특정 시스템(`systemId`)의 특정 시점(`revId`)에 적용되었던 컬럼 매핑 리스트를 복원합니다.

- **Query Parameters**:
  - `systemId`: 원천 시스템 식별자 (ex: SAP_HR)
  - `revId`: 조회 시점 리비전 번호

- **Success Response (200 OK)**:

```JSON
[
  {
    "id": 1,
    "sourceField": "email",
    "targetField": "userName",
    "transformType": "DIRECT"
  },
  {
    "id": 5,
    "sourceField": "status",
    "targetField": "active",
    "transformType": "CUSTOM",
    "transformScript": "new com.iam.core.domain.vo.BooleanData(source.status?.asString() == 'ACTIVE')"
  }
]
```
