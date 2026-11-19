package project.illit.smedia.search.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import project.illit.smedia.common.dto.ApiResponse;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.ratelimit.RateLimit;
import project.illit.smedia.common.security.CustomUserDetails;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.search.dto.GlobalSearchResponse;
import project.illit.smedia.search.dto.SearchSuggestionsResponse;
import project.illit.smedia.search.service.SearchService;
import project.illit.smedia.user.dto.ArtistProfileResponse;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @GetMapping
    public ResponseEntity<ApiResponse<GlobalSearchResponse>> globalSearch(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("q") String q,
            @RequestParam(required = false) Integer fandomId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.globalSearch(q, fandomId, limit, userId)
        ));
    }

    @RateLimit(limit = 45, windowSeconds = 60)
    @GetMapping("/fandoms")
    public ResponseEntity<ApiResponse<PageResponse<FandomSummaryResponse>>> searchFandoms(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("q") String q,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.searchFandoms(q, companyId, userId, pageable)
        ));
    }

    @RateLimit(limit = 45, windowSeconds = 60)
    @GetMapping("/artists")
    public ResponseEntity<ApiResponse<PageResponse<ArtistProfileResponse>>> searchArtists(
            @RequestParam("q") String q,
            @RequestParam(required = false) Integer companyId,
            @RequestParam(required = false) Integer fandomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.searchArtists(q, companyId, fandomId, pageable)
        ));
    }

    @RateLimit(limit = 45, windowSeconds = 60)
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> searchPosts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("q") String q,
            @RequestParam(required = false) Integer fandomId,
            @RequestParam(defaultValue = "ALL") String tab,
            @RequestParam(defaultValue = "false") boolean hasMedia,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.searchPosts(q, fandomId, tab, hasMedia, sortBy, userId, pageable)
        ));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<SearchSuggestionsResponse>> getSuggestions(
            @RequestParam("q") String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.getSuggestions(q, limit)
        ));
    }
}
