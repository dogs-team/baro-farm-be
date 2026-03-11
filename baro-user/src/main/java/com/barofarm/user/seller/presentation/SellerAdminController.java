package com.barofarm.user.seller.presentation;

import com.barofarm.user.seller.application.SellerAdminService;
import com.barofarm.user.seller.domain.Status;
import com.barofarm.user.seller.presentation.dto.admin.AdminSellerApplicationResponse;
import com.barofarm.user.seller.presentation.dto.admin.UpdateSellerApplicationStatusRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.v1}/admin/sellers")
@RequiredArgsConstructor
public class SellerAdminController {

    private final SellerAdminService sellerAdminService;

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminSellerApplicationResponse>> getSellerApplications(
        @RequestParam(required = false) Status sellerStatus,
        Pageable pageable
    ) {
        Page<AdminSellerApplicationResponse> response =
            sellerAdminService.getSellerApplications(sellerStatus, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSellerStatus(
        @PathVariable UUID userId,
        @RequestBody UpdateSellerApplicationStatusRequest request
    ) {
        sellerAdminService.updateSellerStatus(userId, request.sellerStatus(), request.reason());
        return ResponseEntity.ok().build();
    }
}
