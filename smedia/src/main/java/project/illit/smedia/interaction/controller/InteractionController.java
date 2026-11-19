package project.illit.smedia.interaction.controller;

import jakarta.validation.Valid;
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
import project.illit.smedia.interaction.dto.CommentResponse;
import project.illit.smedia.interaction.dto.CreateCommentRequest;
import project.illit.smedia.interaction.dto.ReactionToggleResponse;
import project.illit.smedia.interaction.dto.ReactorResponse;
import project.illit.smedia.interaction.dto.UpdateCommentRequest;
import project.illit.smedia.interaction.service.CommentService;
import project.illit.smedia.interaction.service.ReactionService;
import project.illit.smedia.user.entity.RoleName;

@RestController
@RequestMapping("/api/v1")
public class InteractionController {

    private final ReactionService reactionService;
    private final CommentService commentService;

    public InteractionController(ReactionService reactionService, CommentService commentService) {
        this.reactionService = reactionService;
        this.commentService = commentService;
    }

    // ==================== REACTIONS ====================

    @RateLimit(limit = 60, windowSeconds = 60)
    @PostMapping("/posts/{postId}/reactions/toggle")
    public ResponseEntity<ApiResponse<ReactionToggleResponse>> togglePostReaction(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId
    ) {
        ReactionToggleResponse res = reactionService.togglePostReaction(postId, principal.getId());
        String msg = res.reacted() ? "Post liked" : "Post unliked";
        return ResponseEntity.ok(ApiResponse.ok(msg, res));
    }

    @RateLimit(limit = 60, windowSeconds = 60)
    @PostMapping("/comments/{commentId}/reactions/toggle")
    public ResponseEntity<ApiResponse<ReactionToggleResponse>> toggleCommentReaction(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer commentId
    ) {
        ReactionToggleResponse res = reactionService.toggleCommentReaction(commentId, principal.getId());
        String msg = res.reacted() ? "Comment liked" : "Comment unliked";
        return ResponseEntity.ok(ApiResponse.ok(msg, res));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/posts/{postId}/reactions")
    public ResponseEntity<ApiResponse<PageResponse<ReactorResponse>>> getPostReactors(
            @PathVariable Integer postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(ApiResponse.ok(reactionService.getPostReactors(postId, pageable)));
    }

    // ==================== NESTED 2-LEVEL COMMENTS (ARTIST FIRST) ====================

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getPostComments(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId,
            @RequestParam(defaultValue = "latest") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer viewerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                commentService.getPostComments(postId, sortBy, viewerId, pageable)
        ));
    }

    @RateLimit(limit = 100, windowSeconds = 60)
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getCommentReplies(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Integer viewerId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                commentService.getCommentReplies(commentId, viewerId, pageable)
        ));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer postId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        boolean isAdmin = RoleName.ADMIN.name().equalsIgnoreCase(principal.getRoleName());
        CommentResponse created = commentService.createComment(postId, principal.getId(), isAdmin, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Comment created", created));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer commentId,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        CommentResponse updated = commentService.updateComment(commentId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Comment updated", updated));
    }

    @RateLimit(limit = 20, windowSeconds = 60)
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Integer commentId
    ) {
        boolean isAdmin = RoleName.ADMIN.name().equalsIgnoreCase(principal.getRoleName());
        commentService.deleteComment(commentId, principal.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Comment deleted"));
    }
}
