package project.illit.smedia.fanletter.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.common.security.CustomUserDetails;
import project.illit.smedia.fanletter.dto.CreateFanLetterRequest;
import project.illit.smedia.fanletter.dto.FanLetterResponse;
import project.illit.smedia.fanletter.dto.LetterTemplateRequest;
import project.illit.smedia.fanletter.dto.LetterTemplateResponse;
import project.illit.smedia.fanletter.dto.LetterTemplateStatusRequest;
import project.illit.smedia.fanletter.dto.UpdateFanLetterRequest;
import project.illit.smedia.fanletter.dto.UpdateFanLetterStatusRequest;
import project.illit.smedia.fanletter.service.FanLetterService;
import project.illit.smedia.fanletter.service.LetterTemplateService;
import project.illit.smedia.user.entity.RoleName;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class FanLetterController {

    private final FanLetterService fanLetterService;
    private final LetterTemplateService letterTemplateService;

    public FanLetterController(
            FanLetterService fanLetterService,
            LetterTemplateService letterTemplateService
    ) {
        this.fanLetterService = fanLetterService;
        this.letterTemplateService = letterTemplateService;
    }

    // ==================== LETTER TEMPLATES ====================

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/letter-templates")
    public ResponseEntity<ApiResponse<List<LetterTemplateResponse>>> listActiveTemplates(
            @RequestParam(defaultValue = "false") boolean freeOnly
    ) {
        return ResponseEntity.ok(ApiResponse.ok(letterTemplateService.listActiveTemplates(freeOnly)));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/letter-templates")
    public ResponseEntity<ApiResponse<List<LetterTemplateResponse>>> listAllTemplatesForAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(letterTemplateService.listAllTemplatesForAdmin()));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/letter-templates")
    public ResponseEntity<ApiResponse<LetterTemplateResponse>> createTemplate(
            @Valid @RequestBody LetterTemplateRequest request
    ) {
        LetterTemplateResponse created = letterTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Letter template created", created));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/letter-templates/{templateId}")
    public ResponseEntity<ApiResponse<LetterTemplateResponse>> updateTemplate(
            @PathVariable Integer templateId,
            @Valid @RequestBody LetterTemplateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Letter template updated",
                letterTemplateService.updateTemplate(templateId, request)
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/letter-templates/{templateId}/status")
    public ResponseEntity<ApiResponse<LetterTemplateResponse>> updateTemplateStatus(
            @PathVariable Integer templateId,
            @Valid @RequestBody LetterTemplateStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Letter template status updated",
                letterTemplateService.updateTemplateStatus(templateId, request.isActive())
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/letter-templates/{templateId}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Integer templateId) {
        letterTemplateService.deleteTemplate(templateId);
        return ResponseEntity.ok(ApiResponse.ok("Letter template deleted"));
    }

    // ==================== PUBLIC FAN LETTER WALLS & INBOXES ====================

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}/fan-letters")
    public ResponseEntity<ApiResponse<PageResponse<FanLetterResponse>>> getFandomFanLetterWall(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId,
            @RequestParam(required = false) Integer artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.getFandomPublicWall(fandomId, artistId, isAdmin(principal), pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/artists/{artistUserId}/fan-letters")
    public ResponseEntity<ApiResponse<PageResponse<FanLetterResponse>>> getArtistPublicLetters(
            @PathVariable Integer artistUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.getArtistPublicLetters(artistUserId, pageable)
        ));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ARTIST')")
    @GetMapping("/artists/me/fan-letters")
    public ResponseEntity<ApiResponse<PageResponse<FanLetterResponse>>> getArtistInbox(
            @AuthenticationPrincipal CustomUserDetails artist,
            @RequestParam(defaultValue = "ALL") String visibility,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.getArtistInbox(artist.getId(), visibility, pageable)
        ));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @GetMapping("/fan-letters/me")
    public ResponseEntity<ApiResponse<PageResponse<FanLetterResponse>>> getMySentLetters(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) Integer artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.getMySentLetters(principal.getId(), artistId, pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fan-letters/{letterId}")
    public ResponseEntity<ApiResponse<FanLetterResponse>> getLetterDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer letterId
    ) {
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.getLetterDetail(letterId, userId, isAdmin(principal))
        ));
    }

    // ==================== FAN LETTER CRUD ====================

    @RateLimit(limit = 5, windowSeconds = 60)
    @PostMapping("/fan-letters")
    public ResponseEntity<ApiResponse<FanLetterResponse>> createFanLetter(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateFanLetterRequest request
    ) {
        FanLetterResponse created = fanLetterService.createFanLetter(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Fan letter sent successfully", created));
    }

    @RateLimit(limit = 15, windowSeconds = 60)
    @PutMapping("/fan-letters/{letterId}")
    public ResponseEntity<ApiResponse<FanLetterResponse>> updateFanLetter(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer letterId,
            @Valid @RequestBody UpdateFanLetterRequest request
    ) {
        FanLetterResponse updated = fanLetterService.updateFanLetter(letterId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Fan letter updated successfully", updated));
    }

    @RateLimit(limit = 15, windowSeconds = 60)
    @DeleteMapping("/fan-letters/{letterId}")
    public ResponseEntity<ApiResponse<Void>> deleteFanLetter(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer letterId
    ) {
        fanLetterService.deleteFanLetter(letterId, principal.getId(), isAdmin(principal));
        return ResponseEntity.ok(ApiResponse.ok("Fan letter deleted successfully"));
    }

    // ==================== ADMIN MODERATION ====================

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/fan-letters")
    public ResponseEntity<ApiResponse<PageResponse<FanLetterResponse>>> listLettersForAdmin(
            @RequestParam(required = false) Integer artistId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(
                fanLetterService.listLettersForAdmin(artistId, status, pageable)
        ));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/fan-letters/{letterId}/status")
    public ResponseEntity<ApiResponse<FanLetterResponse>> updateLetterStatus(
            @PathVariable Integer letterId,
            @Valid @RequestBody UpdateFanLetterStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fan letter status updated",
                fanLetterService.updateLetterStatus(letterId, request.status())
        ));
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal != null && RoleName.ADMIN.name().equalsIgnoreCase(principal.getRoleName());
    }
}
