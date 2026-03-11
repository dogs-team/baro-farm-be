package com.barofarm.user.seller.presentation.dto.admin;

import com.barofarm.user.auth.domain.user.User;
import com.barofarm.user.seller.domain.Status;
import java.util.UUID;

public record AdminSellerApplicationResponse(
    UUID userId,
    String email,
    String name,
    String phone,
    User.UserType userType,
    User.UserState userState,
    Status sellerStatus,
    String storeName,
    String businessRegNo,
    String businessOwnerName
) {
}
