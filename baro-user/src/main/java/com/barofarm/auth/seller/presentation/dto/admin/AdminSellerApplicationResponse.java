package com.barofarm.auth.seller.presentation.dto.admin;

import com.barofarm.auth.domain.user.User;
import com.barofarm.auth.seller.domain.Status;
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
