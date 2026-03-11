package com.barofarm.user.seller.application;

import static com.barofarm.user.seller.exception.SellerErrorCode.SELLER_NOT_FOUND;

import com.barofarm.exception.CustomException;
import com.barofarm.user.auth.application.AuthService;
import com.barofarm.user.auth.domain.user.SellerStatus;
import com.barofarm.user.seller.domain.Seller;
import com.barofarm.user.seller.domain.SellerRepository;
import com.barofarm.user.seller.domain.Status;
import com.barofarm.user.seller.infrastructure.SellerJpaRepository;
import com.barofarm.user.seller.presentation.dto.admin.AdminSellerApplicationResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerAdminService {

    /*
     * 관리자 대시보드에서 보는 판매자 신청 목록은 seller 도메인의 조회 모델로 취급한다.
     * user 계정 정보와 seller 신청 상태를 한 번에 조합해 내려주기 위한 read use case다.
     */
    private final SellerJpaRepository sellerJpaRepository;
    private final SellerRepository sellerRepository;
    private final AuthService authService;

    public Page<AdminSellerApplicationResponse> getSellerApplications(
        Status sellerStatus,
        Pageable pageable
    ) {
        return sellerJpaRepository.findAdminSellerApplications(
            sellerStatus,
            normalizePageable(pageable)
        );
    }

    @Transactional
    public void updateSellerStatus(UUID userId, SellerStatus sellerStatus, String reason) {
        Seller seller = sellerRepository.findById(userId)
            .orElseThrow(() -> new CustomException(SELLER_NOT_FOUND));

        /*
         * seller 상태는 같은 서비스 내부 데이터이므로 Kafka 소비를 기다리지 않고
         * 관리자 유스케이스 안에서 즉시 반영한다.
         */
        seller.changeStatus(toSellerDomainStatus(sellerStatus));
        sellerRepository.save(seller);

        /*
         * 권한 변경과 OPA 전파는 auth 도메인의 책임으로 유지한다.
         * 내부 상태 동기화와 외부 상태 전파를 분리해 로컬 Kafka 의존을 줄인다.
         */
        authService.handleSellerStatusChanged(userId, sellerStatus, reason);
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return pageable;
        }

        /*
         * 관리자 화면은 DTO 필드명(userId, sellerStatus) 기준으로 정렬을 요청한다.
         * 하지만 JPA Pageable 정렬은 엔티티 필드명(id, status) 기준으로 해석되므로
         * 목록 전용 정렬 키를 엔티티 필드명으로 보정한다.
         */
        Sort normalizedSort = Sort.by(
            pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList()
        );
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private String mapSortProperty(String property) {
        return switch (property) {
            case "userId" -> "id";
            case "sellerStatus" -> "status";
            default -> property;
        };
    }

    private Status toSellerDomainStatus(SellerStatus sellerStatus) {
        return switch (sellerStatus) {
            case APPROVED -> Status.APPROVED;
            case REJECTED -> Status.REJECTED;
            case SUSPENDED -> Status.SUSPENDED;
        };
    }
}
