package com.example.order.controller;

import com.example.order.domain.CouponStatus;
import com.example.order.dto.*;
import com.example.order.service.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Coupons", description = "Coupon management API")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @Operation(summary = "Create coupon", description = "Create a new coupon policy (admin)")
    @PostMapping
    public ResponseEntity<Long> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        Long couponId = couponService.createCoupon(request);
        return ResponseEntity.created(URI.create("/api/coupons/" + couponId)).body(couponId);
    }

    @Operation(summary = "Update coupon", description = "Update coupon info (Optimistic Lock protected)")
    @PutMapping("/{couponId}")
    public ResponseEntity<Void> updateCoupon(@PathVariable Long couponId,
                                             @Valid @RequestBody UpdateCouponRequest request) {
        couponService.updateCoupon(couponId, request);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete coupon", description = "Soft delete — hidden from queries but data preserved")
    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get coupon detail")
    @GetMapping("/{couponId}")
    public ResponseEntity<CouponResponse> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(couponService.findCoupon(couponId));
    }

    @Operation(summary = "Search coupons", description = "Dynamic search with pagination (QueryDSL Projection)")
    @GetMapping
    public ResponseEntity<Page<CouponSummaryDto>> searchCoupons(
            CouponSearchCondition condition,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(couponService.searchCoupons(condition, pageable));
    }

    // === Coupon Issuance === //

    @Operation(summary = "Issue coupon to member", description = "Pessimistic Lock prevents over-issuance")
    @PostMapping("/{couponId}/issue")
    public ResponseEntity<Long> issueCoupon(@PathVariable Long couponId,
                                            @RequestParam Long memberId) {
        Long memberCouponId = couponService.issueCoupon(couponId, memberId);
        return ResponseEntity.ok(memberCouponId);
    }

    // === Member Coupon Queries === //

    @Operation(summary = "Get member's coupons", description = "Optional filter by status (ISSUED, USED, EXPIRED)")
    @GetMapping("/members/{memberId}")
    public ResponseEntity<List<MemberCouponResponse>> getMemberCoupons(
            @PathVariable Long memberId,
            @RequestParam(required = false) CouponStatus status) {
        return ResponseEntity.ok(couponService.findMemberCoupons(memberId, status));
    }
}
