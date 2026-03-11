# seller 도메인 경계 정리 메모

## 왜 정리했는가

`baro-user`는 배포 단위로는 하나의 서비스지만, 내부에는 성격이 다른 하위 도메인이 함께 들어 있다.

- `auth`: 회원가입, 로그인, 비밀번호, 토큰, OAuth, 계정 상태
- `seller`: 판매자 신청, 판매자 프로필, 판매자 신청 상태, 관리자 승인/거절/정지

기존에는 판매자 관리자 기능 일부가 `auth` 쪽 컨트롤러에 들어가 있어서 책임 경계가 흐려져 있었다.
DDD와 클린 아키텍처 관점에서는 판매자 신청 목록 조회와 판매자 상태 변경은 seller 도메인이 소유하는 편이 더 자연스럽다.

## 이번 리팩터링 범위

- 루트 패키지를 `com.barofarm.user`로 정리
- `UserApplication`을 서비스 루트인 `com.barofarm.user` 바로 아래로 이동
- auth 패키지를 `com.barofarm.user.auth`로 이동
- seller 패키지를 `com.barofarm.user.seller`로 이동
- 관리자용 판매자 신청 목록 조회 API를 seller 도메인으로 배치
- 관리자용 판매자 상태 변경 API를 seller 도메인으로 배치

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
  - 계정 관리 중심의 auth 관리자 API
- `GET /api/v1/admin/sellers/applications`
  - 관리자 화면용 판매자 신청 목록 조회 API
- `POST /api/v1/admin/sellers/{userId}/status`
  - 관리자 화면용 판매자 상태 변경 API
- `POST /api/v1/sellers/apply`
  - 판매자 신청 API
- `GET /api/v1/sellers/sellerInfo/{userId}`
  - 판매자 단건 조회 API
- `POST /api/v1/sellers/sellerInfo/bulks`
  - 판매자 일괄 조회 API

즉 판매자 신청과 판매자 관리자 기능은 seller 도메인이 소유하고, 계정 자체의 인증과 계정 상태는 auth 도메인이 소유하도록 정리했다.

## 과도기 메모

현재 판매자 상태 변경은 내부적으로 `AuthService.updateSellerStatus(...)`를 호출한다.
이유는 판매자 승인 시 사용자 역할 승격과 OPA hotlist 반영 로직이 아직 auth 쪽에 있기 때문이다.

이번 변경은 우선 진입점과 유스케이스 소유권을 seller 도메인으로 옮기는 1차 정리다.
다음 단계에서는 seller가 auth 내부 구현을 직접 호출하지 않도록 포트나 퍼사드로 의존 방향을 정리할 필요가 있다.
