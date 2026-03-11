# seller 도메인 경계 정리 메모

## 왜 정리했는가

`baro-user`는 물리적으로 하나의 서비스지만, 논리적으로는 서로 다른 두 도메인을 함께 담고 있다.

- `auth`: 회원가입, 로그인, 비밀번호, 토큰, OAuth, 계정 상태
- `seller`: 판매자 신청, 판매자 프로필, 판매자 신청 상태, 승인/거절/정지

기존에는 판매자 관리자 기능이 `AuthController` 안에 섞여 있어,
판매자 신청 목록 조회와 상태 변경의 소유권이 `auth`에 있는 것처럼 보였다.
DDD / 클린 아키텍처 관점에서는 이 책임이 seller 도메인에 있는 편이 더 자연스럽다.

## 이번 리팩터링 범위

- 관리자용 판매자 신청 목록 조회 API를 `seller.presentation.SellerAdminController`로 이동
- 관리자용 판매자 상태 변경 API도 seller 쪽 controller로 이동
- 관리자 대시보드용 응답 모델을 `seller.presentation.dto.admin`에 분리
- seller 도메인의 조회 유스케이스를 `seller.application.SellerAdminService`로 분리

## 경계 판단

- `GET /api/v1/auth/admin/users` 는 계정 관리용 auth API로 유지
- 판매자 신청 목록은 seller 도메인의 read model로 별도 제공
- 관리자 대시보드에서 필요한 seller 관련 조회/상태변경은 seller가 소유

## 과도기 메모

현재 seller 상태 변경은 여전히 `AuthService.updateSellerStatus(...)`를 호출한다.
이유는 권한 승격과 OPA hotlist 반영 로직이 아직 auth 쪽에 있기 때문이다.

즉 이번 변경은 controller / use case 경계를 먼저 정리한 1차 리팩터링이고,
다음 단계에서는 seller가 auth에 직접 의존하지 않도록 포트/파사드로 끊어내는 작업이 필요하다.
