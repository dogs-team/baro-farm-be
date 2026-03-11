package com.barofarm.auth.seller.application;

import com.barofarm.auth.application.AuthService;
import com.barofarm.auth.domain.user.SellerStatus;
import com.barofarm.auth.seller.domain.Status;
import com.barofarm.auth.seller.infrastructure.SellerJpaRepository;
import com.barofarm.auth.seller.presentation.dto.admin.AdminSellerApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAdminService {

    /*
     * 관리자 대시보드에서 보는 판매자 신청 목록은 seller 도메인의 조회 모델로 취급한다.
     * user 계정 정보와 seller 신청 상태를 한 번에 조합해 내려주기 위한 read use case다.
     *
     * 상태 변경은 아직 AuthService가 권한 승격과 OPA hotlist 반영까지 함께 처리하고 있으므로,
     * seller 관리자 유스케이스에서는 우선 그 흐름을 호출하는 전이 계층으로 둔다.
     * 이후 경계를 더 다듬을 때 seller 쪽 포트/파사드로 분리하는 것이 다음 단계다.
     */
    private final SellerJpaRepository sellerJpaRepository;
    private final AuthService authService;

    public Page<AdminSellerApplicationResponse> getSellerApplications(
        Status sellerStatus,
        Pageable pageable
    ) {
        return sellerJpaRepository.findAdminSellerApplications(sellerStatus, pageable);
    }

    @Transactional
    public void updateSellerStatus(UUID userId, SellerStatus sellerStatus, String reason) {
        authService.updateSellerStatus(userId, sellerStatus, reason);
    }
}
