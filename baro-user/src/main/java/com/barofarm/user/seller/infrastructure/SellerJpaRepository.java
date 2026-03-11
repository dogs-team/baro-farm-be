package com.barofarm.user.seller.infrastructure;

import com.barofarm.user.seller.domain.Seller;
import com.barofarm.user.seller.domain.Status;
import com.barofarm.user.seller.presentation.dto.admin.AdminSellerApplicationResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SellerJpaRepository extends JpaRepository<Seller, UUID> {
    boolean existsByBusinessRegNo(String businessRegNo);
    List<Seller> findByIdIn(List<UUID> ids);

    @Query(
        value = """
            select new com.barofarm.user.seller.presentation.dto.admin.AdminSellerApplicationResponse(
                u.id,
                u.email,
                u.name,
                u.phone,
                u.userType,
                u.userState,
                s.status,
                s.storeName,
                s.businessRegNo,
                s.businessOwnerName
            )
            from User u, Seller s
            where s.id = u.id
              and (:sellerStatus is null or s.status = :sellerStatus)
            """,
        countQuery = """
            select count(s.id)
            from User u, Seller s
            where s.id = u.id
              and (:sellerStatus is null or s.status = :sellerStatus)
            """
    )
    Page<AdminSellerApplicationResponse> findAdminSellerApplications(
        @Param("sellerStatus") Status sellerStatus,
        Pageable pageable
    );
}
