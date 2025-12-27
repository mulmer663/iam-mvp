# IAM API Specification

이 문서는 IAM 시스템의 API 명세를 정의합니다. 모든 API는 RESTful 원칙을 따르며, 응답 데이터 포맷은 JSON을 사용합니다.

## 1. 공통 응답 구조 (Error Response)

에러 발생 시 다음과 같은 공통 구조로 응답합니다.

### Error Response Type (TypeScript)

```typescript
interface ErrorResponse {
  errorCode: string;    // 내부 에러 코드 (ex: IAM-4100)
  message: string;      // 사용자 친화적 메시지
  detail?: string;      // 상세 에러 정보 (디버깅용)
  traceId: string;      // 로그 추적용 ID
  timestamp: string;    // 발생 시각 (ISO 8601)
  path: string;         // 요청 경로
  status: number;       // HTTP 상태 코드
  fieldErrors?: {       // 필드 검증 실패 리스트
    field: string;
    rejectedValue: any;
    message: string;
    code: string;
  }[];
}
```

### Common Error Cases

- `400 Bad Request`: 요청 형식이 잘못되었거나 필수 필드 누락
- `401 Unauthorized`: 인증 실패
- `404 Not Found`: 리소스가 존재하지 않음
- `500 Internal Server Error`: 서버 내부 오류

---

## 2. SCIM User API

SCIM 2.0 표준을 준수하는 사용자 관리 API입니다.

### 2.1 사용자 목록 조회

- **Endpoint**: `GET /scim/v2/Users`
- **Description**: 전체 사용자 목록을 SCIM 형식으로 조회합니다.
- **Success Response (200 OK)**

  ```json
  {
    "schemas": ["urn:ietf:params:scim:api:messages:2.0:ListResponse"],
    "totalResults": 1,
    "itemsPerPage": 1,
    "startIndex": 1,
    "Resources": [
      {
        "id": "1",
        "userName": "johndoe",
        "name": {
          "familyName": "John",
          "givenName": "Doe",
          "formatted": "John Doe"
        },
        "title": "Software Engineer",
        "active": true
      }
    ]
  }
  ```

### 2.2 사용자 상세 조회

- **Endpoint**: `GET /scim/v2/Users/{id}`
- **Description**: 특정 사용자의 상세 정보를 조회합니다.
- **Success Response (200 OK)**

  ```json
  {
    "schemas": [
      "urn:ietf:params:scim:schemas:core:2.0:User",
      "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"
    ],
    "id": "1",
    "externalId": "HR-001",
    "userName": "johndoe@example.com",
    "name": {
      "familyName": "John",
      "givenName": "Doe",
      "formatted": "John Doe"
    },
    "title": "Software Engineer",
    "emails": [
      {
        "value": "johndoe@example.com",
        "primary": true
      }
    ],
    "active": true,
    "meta": {
      "resourceType": "User",
      "created": "2023-10-27T10:00:00Z",
      "lastModified": "2023-10-27T10:00:00Z",
      "location": "/scim/v2/Users/1"
    }
  }
  ```

### TypeScript Type (ScimUser)

```typescript
interface ScimUser {
  schemas: string[];
  id: string;
  externalId?: string;
  userName: string;
  name: {
    familyName: string;
    givenName: string;
    formatted: string;
  };
  title?: string;
  emails: {
    value: string;
    primary: boolean;
  }[];
  active: boolean;
  meta: {
    resourceType: string;
    created: string;
    lastModified: string;
    location: string;
  };
  "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User"?: {
    employeeNumber?: string;
    department?: string;
    costCenter?: string;
    organization?: string;
    division?: string;
  };
}
```

---

## 3. History API

시스템 동기화 이력 및 변경 로그를 조회하는 API입니다.

### 3.1 동기화 이력 조회

- **Endpoint**: `GET /api/v1/history`
- **Description**: 전체 동기화 이력 로그를 조회합니다.
- **Success Response (200 OK)**

  ```json
  [
    {
      "id": "01H...",
      "traceId": "abc-123",
      "type": "USER_SYNC",
      "status": "SUCCESS",
      "target": "HR",
      "time": "2023-10-27T10:30:00",
      "message": "User created successfully",
      "payload": "{\"userName\":\"johndoe\"}"
    }
  ]
  ```

### TypeScript Type (HistoryLog)

```typescript
interface HistoryLog {
  id: string;
  traceId: string;
  type: string;
  status: string;
  target: string;
  time: string;
  message: string;
  payload?: string;
}
```
