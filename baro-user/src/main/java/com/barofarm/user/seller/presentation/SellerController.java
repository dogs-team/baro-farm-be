package com.barofarm.user.seller.presentation;

import com.barofarm.dto.ResponseDto;
import com.barofarm.user.seller.application.SellerService;
import com.barofarm.user.seller.presentation.dto.SellerApplyRequestDto;
import com.barofarm.user.seller.presentation.dto.SellerInfoDto;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${api.v1}/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @PostMapping("/apply")
    public ResponseDto<Void> applyForSeller(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody SellerApplyRequestDto request
    ) {
        sellerService.applyForSeller(userId, request);
        return ResponseDto.ok(null);
    }

    // 단건 판매자 프로필 조회
    @GetMapping("/sellerInfo/{userId}")
    public ResponseDto<SellerInfoDto> getByUserId(@PathVariable("userId") UUID userId) {
        SellerInfoDto info = sellerService.getASellerByUserId(userId);
        return ResponseDto.ok(info);
    }

    // 여러 사용자에 대한 판매자 프로필 일괄 조회
    @PostMapping("/sellerInfo/bulks")
    public ResponseDto<List<SellerInfoDto>> getByUsers(@RequestBody List<UUID> userIds) {
        List<SellerInfoDto> infos = sellerService.getSellersByIds(userIds);
        return ResponseDto.ok(infos);
    }
}
