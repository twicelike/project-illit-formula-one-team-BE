package project.illit.smedia.user.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.common.security.CustomUserDetails;
import project.illit.smedia.user.dto.FollowActionResponse;
import project.illit.smedia.user.dto.FollowStatusResponse;
import project.illit.smedia.user.dto.FollowUserResponse;
import project.illit.smedia.user.service.FollowService;

@RestController
@RequestMapping("/api/v1/users")
public class FollowController {

    private final FollowService followService;

    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PostMapping("/{targetUserId}/follow")
    public ResponseEntity<ApiResponse<FollowActionResponse>> followUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer targetUserId
    ) {
        FollowActionResponse res = followService.followUser(principal.getId(), targetUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Followed user successfully", res));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<ApiResponse<FollowActionResponse>> unfollowUser(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer targetUserId
    ) {
        FollowActionResponse res = followService.unfollowUser(principal.getId(), targetUserId);
        return ResponseEntity.ok(ApiResponse.ok("Unfollowed user successfully", res));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getFollowers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Integer viewerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowers(userId, viewerId, pageable)));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<PageResponse<FollowUserResponse>>> getFollowing(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Integer viewerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowing(userId, viewerId, pageable)));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/{targetUserId}/follow-status")
    public ResponseEntity<ApiResponse<FollowStatusResponse>> getFollowStatus(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer targetUserId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(followService.getFollowStatus(principal.getId(), targetUserId)));
    }
}
