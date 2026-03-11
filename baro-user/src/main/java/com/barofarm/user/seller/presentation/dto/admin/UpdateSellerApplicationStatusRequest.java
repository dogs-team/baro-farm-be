package com.barofarm.user.seller.presentation.dto.admin;

import com.barofarm.user.auth.domain.user.SellerStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSellerApplicationStatusRequest(
    @NotNull SellerStatus sellerStatus,
    String reason
) {
}
