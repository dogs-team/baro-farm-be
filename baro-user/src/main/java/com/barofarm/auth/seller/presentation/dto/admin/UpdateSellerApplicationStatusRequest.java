package com.barofarm.auth.seller.presentation.dto.admin;

import com.barofarm.auth.domain.user.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerApplicationStatusRequest(
    @NotNull SellerStatus sellerStatus,
    String reason
) {
}
