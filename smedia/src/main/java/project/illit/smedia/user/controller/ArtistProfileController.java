package project.illit.smedia.user.controller;

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
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.dto.CreateArtistAccountRequest;
import project.illit.smedia.user.dto.UpdateArtistProfileRequest;
import project.illit.smedia.user.service.ArtistProfileService;

@RestController
@RequestMapping("/api/v1")
public class ArtistProfileController {

    private final ArtistProfileService artistProfileService;

    public ArtistProfileController(ArtistProfileService artistProfileService) {
        this.artistProfileService = artistProfileService;
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/artists")
    public ResponseEntity<ApiResponse<PageResponse<ArtistProfileResponse>>> browseArtists(
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "stageName"));
        return ResponseEntity.ok(ApiResponse.ok(artistProfileService.listArtists(companyId, keyword, pageable)));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/artists/{userId}")
    public ResponseEntity<ApiResponse<ArtistProfileResponse>> getArtistDetail(@PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(artistProfileService.getArtistDetail(userId)));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PreAuthorize("hasRole('ARTIST')")
    @PutMapping("/artists/me")
    public ResponseEntity<ApiResponse<ArtistProfileResponse>> updateMyArtistProfile(
            @AuthenticationPrincipal CustomUserDetails artist,
            @Valid @RequestBody UpdateArtistProfileRequest request
    ) {
        ArtistProfileResponse updated = artistProfileService.updateMyArtistProfile(artist.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Artist profile updated", updated));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/artists")
    public ResponseEntity<ApiResponse<ArtistProfileResponse>> provisionArtist(
            @Valid @RequestBody CreateArtistAccountRequest request
    ) {
        ArtistProfileResponse created = artistProfileService.provisionArtistAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Artist account created", created));
    }
}
