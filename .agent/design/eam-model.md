# EAM 모델 설계 메모

## 목표 아키텍처

장기적으로 EAM(Enterprise Access Management) 구조를 향함:

```
User → Group → Role → Resource(권한 집합)
```

## Department vs Group 논의 (2026-04-28)

### 배경
부서 기반 정책 사이트에서는 `Department = Group`이 되고, 부서에 Role이 직접 연결될 수 있음.
이 경우 SCIM Group 리소스로 Department를 표현하면 User-Group-Role 체인이 자연스럽게 흡수됨.

### 선택지

| | Department = Group 서브타입 | Department = 별도 리소스 |
|---|---|---|
| SCIM 경로 | `/scim/v2/Groups` + `type=department` | `/scim/v2/Departments` |
| 계층 표현 | Group 확장 필드 필요 | `parentId` 1등 시민 |
| Role 연결 (미래) | Group→Role 체인 자동 흡수 | Department→Role 별도 연결 |
| 보안그룹과 혼재 | `type`으로 구분 필요 | 분리 깔끔 |

### 현재 결정 (MVP)

**Department를 별도 리소스 타입으로 유지** (`ScimDynamicResource` 기반, `/scim/v2/Departments`).

근거: Role과 Resource 개념이 아직 없음. 도착지 없는 상태에서 연결 구조를 미리 맞추는 건 추측 설계.
`ScimEndpointConstants.DEPARTMENTS`와 `AttributeTargetDomain.DEPARTMENT`가 이미 선언되어 있어 인프라 준비됨.

### 재검토 시점

Role 도입 단계에서 재논의:
- Group이 Department를 상속할지
- Group과 Department가 공통 `Principal` 추상화를 가질지
