package project.illit.smedia.user.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.common.security.CustomUserDetails;
import project.illit.smedia.user.dto.ChangePasswordRequest;
import project.illit.smedia.user.dto.UpdateProfileRequest;
import project.illit.smedia.user.dto.UpdateUserStatusRequest;
import project.illit.smedia.user.dto.UserProfileResponse;
import project.illit.smedia.user.service.UserProfileService;

@RestController
@RequestMapping("/api/v1")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.getMyProfile(principal.getId())));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse updated = userProfileService.updateMyProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", updated));
    }

    @RateLimit(limit = 5, windowSeconds = 300)
    @PutMapping("/users/me/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userProfileService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Password updated successfully"));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getPublicProfile(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.getPublicProfile(userId)));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse<PageResponse<UserProfileResponse>>> listAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(userProfileService.listUsersForAdmin(role, pageable)));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/users/{userId}/status")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserStatus(
            @AuthenticationPrincipal CustomUserDetails admin,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        UserProfileResponse updated = userProfileService.updateUserStatus(
                admin.getId(), userId, request.isActive()
        );
        return ResponseEntity.ok(ApiResponse.ok("User status updated", updated));
    }
}
