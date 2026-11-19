package project.illit.smedia.post.controller;

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
import project.illit.smedia.post.dto.CreatePostRequest;
import project.illit.smedia.post.dto.FandomMediaGalleryItem;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.post.dto.UpdatePostRequest;
import project.illit.smedia.post.dto.UpdatePostStatusRequest;
import project.illit.smedia.post.service.PostService;
import project.illit.smedia.user.entity.RoleName;

@RestController
@RequestMapping("/api/v1")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // ==================== DUAL FEED (FEED / FAN / ARTIST) & MEDIA GALLERY ====================

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getFandomFeed(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId,
            @RequestParam(defaultValue = "FEED") String tab,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "false") boolean hasMedia,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                postService.getFandomFeed(fandomId, tab, sortBy, hasMedia, userId, isAdmin(principal), pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/fandoms/{fandomId}/media")
    public ResponseEntity<ApiResponse<PageResponse<FandomMediaGalleryItem>>> getFandomMediaGallery(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId,
            @RequestParam(defaultValue = "false") boolean artistOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 60));
        return ResponseEntity.ok(ApiResponse.ok(
                postService.getFandomMediaGallery(fandomId, artistOnly, isAdmin(principal), pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> getUserPosts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer viewerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                postService.getUserPosts(userId, viewerId, pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPostDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId
    ) {
        Integer userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                postService.getPostDetail(postId, userId, isAdmin(principal))
        ));
    }

    // ==================== POST CRUD ====================

    @RateLimit(limit = 10, windowSeconds = 60)
    @PostMapping("/fandoms/{fandomId}/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer fandomId,
            @Valid @RequestBody CreatePostRequest request
    ) {
        PostResponse created = postService.createPost(fandomId, principal.getId(), isAdmin(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Post published successfully", created));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId,
            @Valid @RequestBody UpdatePostRequest request
    ) {
        PostResponse updated = postService.updatePost(postId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Post updated successfully", updated));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId
    ) {
        postService.deletePost(postId, principal.getId(), isAdmin(principal));
        return ResponseEntity.ok(ApiResponse.ok("Post deleted successfully"));
    }

    // ==================== ADMIN MODERATION ====================

    @RateLimit(limit = 60, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/posts")
    public ResponseEntity<ApiResponse<PageResponse<PostResponse>>> listPostsForAdmin(
            @RequestParam(required = false) Integer fandomId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(postService.listPostsForAdmin(fandomId, status, pageable)));
    }

    @RateLimit(limit = 30, windowSeconds = 60)
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/posts/{postId}/status")
    public ResponseEntity<ApiResponse<PostResponse>> updatePostStatus(
            @PathVariable Integer postId,
            @Valid @RequestBody UpdatePostStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Post status updated",
                postService.updatePostStatus(postId, request.status())
        ));
    }

    private boolean isAdmin(CustomUserDetails principal) {
        return principal != null && RoleName.ADMIN.name().equalsIgnoreCase(principal.getRoleName());
    }
}
