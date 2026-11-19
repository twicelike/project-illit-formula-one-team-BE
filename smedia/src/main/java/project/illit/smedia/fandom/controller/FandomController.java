package project.illit.smedia.fandom.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.common.security.CustomUserDetails;
import project.illit.smedia.fandom.dto.AssignArtistRequest;
import project.illit.smedia.fandom.dto.CreateFandomRequest;
import project.illit.smedia.fandom.dto.FandomDetailResponse;
import project.illit.smedia.fandom.dto.FandomMemberResponse;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.fandom.dto.MyCommunityResponse;
import project.illit.smedia.fandom.dto.UpdateFandomMemberRoleRequest;
import project.illit.smedia.fandom.dto.UpdateFandomRequest;
import project.illit.smedia.fandom.dto.UpdateFandomStatusRequest;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.entity.RoleName;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FandomController {

    private final FandomService fandomService;

    public FandomController(FandomService fandomService) {
        this.fandomService = fandomService;
    }

    // ==================== PUBLIC / DISCOVER & COMMUNITY DETAIL ====================

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms")
    public ResponseEntity<ApiResponse<PageResponse<FandomSummaryResponse>>> discoverFandoms(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "newest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Sort sort = "name".equalsIgnoreCase(sortBy)
                ? Sort.by(Sort.Direction.ASC, "name")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        Integer userId = principal != null ? principal.getId() : null;

        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.discoverFandoms(companyId, keyword, userId, pageable)
        ));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @GetMapping("/fandoms/me")
    public ResponseEntity<ApiResponse<PageResponse<MyCommunityResponse>>> getMyCommunities(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.getMyCommunities(principal.getId(), pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}")
    public ResponseEntity<ApiResponse<FandomDetailResponse>> getFandomDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId
    ) {
        Integer userId = principal != null ? principal.getId() : null;
        boolean isAdmin = isAdmin(principal);
        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.getFandomDetail(fandomId, userId, isAdmin)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}/artists")
    public ResponseEntity<ApiResponse<List<ArtistProfileResponse>>> getFandomArtists(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.getFandomArtists(fandomId, isAdmin(principal))
        ));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}/members")
    public ResponseEntity<ApiResponse<PageResponse<FandomMemberResponse>>> getFandomMembers(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "joinedAt"));
        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.listFandomMembers(fandomId, role, isAdmin(principal), pageable)
        ));
    }

    // ==================== JOIN / LEAVE COMMUNITY ====================

    @RateLimit(limit = 15, windowSeconds = 60)
    @PostMapping("/fandoms/{fandomId}/join")
    public ResponseEntity<ApiResponse<FandomMemberResponse>> joinFandom(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId
    ) {
        FandomMemberResponse joined = fandomService.joinFandom(fandomId, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Joined fandom successfully", joined));
    }

    @RateLimit(limit = 15, windowSeconds = 60)
    @DeleteMapping("/fandoms/{fandomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveFandom(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId
    ) {
        fandomService.leaveFandom(fandomId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Left fandom successfully"));
    }

    // ==================== ADMIN FANDOM MANAGEMENT ====================

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/fandoms")
    public ResponseEntity<ApiResponse<PageResponse<FandomSummaryResponse>>> listFandomsForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(
                fandomService.listFandomsForAdmin(status, companyId, keyword, pageable)
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/fandoms")
    public ResponseEntity<ApiResponse<FandomSummaryResponse>> createFandom(
            @Valid @RequestBody CreateFandomRequest request
    ) {
        FandomSummaryResponse created = fandomService.createFandom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fandom created", created));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/fandoms/{fandomId}")
    public ResponseEntity<ApiResponse<FandomSummaryResponse>> updateFandom(
            @PathVariable Integer fandomId,
            @Valid @RequestBody UpdateFandomRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fandom updated",
                fandomService.updateFandom(fandomId, request)
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/fandoms/{fandomId}/status")
    public ResponseEntity<ApiResponse<FandomSummaryResponse>> updateFandomStatus(
            @PathVariable Integer fandomId,
            @Valid @RequestBody UpdateFandomStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fandom status updated",
                fandomService.updateFandomStatus(fandomId, request.status())
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/fandoms/{fandomId}")
    public ResponseEntity<ApiResponse<Void>> deleteFandom(@PathVariable Integer fandomId) {
        fandomService.deleteFandom(fandomId);
        return ResponseEntity.ok(ApiResponse.ok("Fandom deleted"));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/fandoms/{fandomId}/artists")
    public ResponseEntity<ApiResponse<FandomMemberResponse>> assignArtistToFandom(
            @PathVariable Integer fandomId,
            @Valid @RequestBody AssignArtistRequest request
    ) {
        FandomMemberResponse assigned = fandomService.assignArtistToFandom(fandomId, request.artistUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Artist assigned to fandom", assigned));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/fandoms/{fandomId}/artists/{artistUserId}")
    public ResponseEntity<ApiResponse<Void>> removeArtistFromFandom(
            @PathVariable Integer fandomId,
            @PathVariable Integer artistUserId
    ) {
        fandomService.removeArtistFromFandom(fandomId, artistUserId);
        return ResponseEntity.ok(ApiResponse.ok("Artist removed from fandom"));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/fandoms/{fandomId}/members/{userId}/role")
    public ResponseEntity<ApiResponse<FandomMemberResponse>> updateMemberRole(
            @PathVariable Integer fandomId,
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateFandomMemberRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fandom member role updated",
                fandomService.updateMemberRole(fandomId, userId, request.role())
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/fandoms/{fandomId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable Integer fandomId,
            @PathVariable Integer userId
    ) {
        fandomService.removeMember(fandomId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Member removed from fandom"));
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal != null && RoleName.ADMIN.name().equalsIgnoreCase(principal.getRoleName());
    }
}
