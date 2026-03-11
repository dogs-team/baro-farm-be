# seller 도메인 경계 정리 메모

## 왜 정리했는가

`baro-user`는 배포 단위로는 하나의 서비스지만, 내부에는 성격이 다른 두 도메인이 함께 있다.

- `auth`: 회원가입, 로그인, 비밀번호, 토큰, OAuth, 계정 상태
- `seller`: 판매자 신청, 판매자 프로필, 판매자 신청 상태, 관리자 승인/거절/정지

기존에는 판매자 관리자 기능 일부가 `auth` 쪽에 남아 있었고, 판매자 상태 동기화도 Kafka 소비에 의존하고 있었다.
서비스가 이미 하나로 합쳐진 현재 구조에서는 이 경계가 어색했고, 로컬 개발 환경에서도 불필요하게 Kafka 가용성에 영향을 받았다.

## 이번 리팩터링 범위

- 루트 패키지를 `com.barofarm.user`로 정리
- `auth`와 `seller`를 같은 레벨의 도메인 패키지로 분리
- 관리자용 판매자 신청 목록/상태 변경 API를 `seller` 도메인으로 이동
- seller 상태 변경 시 내부 seller 테이블 갱신은 Kafka 소비가 아니라 seller 도메인에서 직접 처리
- Kafka는 OPA hotlist 같은 외부 상태 전파 용도로만 유지

## 최종 패키지 구조

```text
com.barofarm.user
├─ UserApplication
├─ auth
│  ├─ application
│  ├─ common
│  ├─ domain
│  ├─ exception
│  ├─ infrastructure
│  └─ presentation
└─ seller
   ├─ application
   ├─ config
   ├─ domain
   ├─ exception
   ├─ infrastructure
   └─ presentation
```

## API 경계 정리

- `GET /api/v1/auth/admin/users`
  - 계정 중심의 관리자 조회 API
- `GET /api/v1/admin/sellers/applications`
  - 관리자 화면의 판매자 신청 목록 조회 API
- `POST /api/v1/admin/sellers/{userId}/status`
  - 관리자 화면의 판매자 상태 변경 API
- `POST /api/v1/sellers/apply`
  - 판매자 신청 API
- `GET /api/v1/sellers/sellerInfo/{userId}`
  - 판매자 단건 조회 API
- `POST /api/v1/sellers/sellerInfo/bulks`
  - 판매자 벌크 조회 API

즉 판매자 신청과 판매자 관리자 기능은 `seller` 도메인이 소유하고, 계정 자체의 인증/인가와 계정 상태는 `auth` 도메인이 소유하도록 정리했다.

## Kafka 역할 재정리

현재 `user-service`에서 Kafka는 두 역할로 나뉜다.

1. OPA hotlist 같은 외부 권한 상태 전파
2. 일부 도메인 이벤트의 비동기 발행

반면 같은 서비스 내부의 seller 상태 동기화는 Kafka가 반드시 필요하지 않다.
그래서 이번 변경에서는 seller 상태를 관리자 유스케이스 안에서 즉시 반영하고, 그 다음 auth 도메인에서 권한 갱신과 OPA 전파만 처리하도록 나눴다.

## 현재 과도기 상태

판매자 상태 변경 후처리는 아직 `AuthService.handleSellerStatusChanged(...)`가 담당한다.
이 메서드는 다음 책임만 가진다.

- 승인 시 `SELLER` 권한 부여
- OPA hotlist 이벤트 발행

즉 내부 seller 데이터 변경은 seller 도메인에서 처리하고, 외부 반영은 auth 도메인에서 처리하는 형태로 경계를 한 단계 정리한 상태다.

## 다음 단계 TODO

- seller 상태 변경 후처리를 auth 퍼사드나 포트로 더 명확히 분리
- `grantSeller` 같은 과거 호환용 흐름 정리
- 내부 동기화와 외부 이벤트 전파를 더 분명히 나눠 문서화
