package project.illit.smedia.interaction.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ForbiddenException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.FandomMemberRole;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.interaction.dto.CommentResponse;
import project.illit.smedia.interaction.dto.CreateCommentRequest;
import project.illit.smedia.interaction.dto.UpdateCommentRequest;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.interaction.entity.ReactionTargetType;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.dto.PostAuthorResponse;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.entity.PostStatus;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CommentService {

    private static final int PREVIEW_REPLIES_LIMIT = 2;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ReactionRepository reactionRepository;
    private final FandomMemberRepository fandomMemberRepository;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public CommentService(
            CommentRepository commentRepository,
            PostRepository postRepository,
            ReactionRepository reactionRepository,
            FandomMemberRepository fandomMemberRepository,
            UserRepository userRepository,
            ArtistProfileRepository artistProfileRepository
    ) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.reactionRepository = reactionRepository;
        this.fandomMemberRepository = fandomMemberRepository;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getPostComments(
            Integer postId,
            String sortBy,
            Integer currentUserId,
            Pageable pageable
    ) {
        Post post = postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        Page<Comment> topPage = resolveTopLevelSort(sortBy)
                ? commentRepository.findTopLevelCommentsArtistFirstPopular(postId, pageable)
                : commentRepository.findTopLevelCommentsArtistFirstLatest(postId, pageable);

        List<Comment> topComments = topPage.getContent();
        if (topComments.isEmpty()) {
            return PageResponse.<CommentResponse>builder()
                    .items(List.of())
                    .page(topPage.getNumber())
                    .size(topPage.getSize())
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        List<Integer> topIds = topComments.stream().map(Comment::getId).toList();
        List<Comment> allReplies = commentRepository.findRepliesByParentIdsArtistFirst(topIds);

        Map<Integer, List<Comment>> repliesByParent = new HashMap<>();
        Set<Integer> parentsWithArtistReply = new HashSet<>();
        for (Comment reply : allReplies) {
            Integer pid = reply.getParent().getId();
            repliesByParent.computeIfAbsent(pid, ignored -> new ArrayList<>()).add(reply);
            if (isArtistUser(reply.getUser())) {
                parentsWithArtistReply.add(pid);
            }
        }

        // Gather IDs for preview replies + top comments so we batch-load artist profiles & viewer reactions once
        List<Comment> previewRepliesFlat = new ArrayList<>();
        Map<Integer, List<Comment>> previewRepliesByParent = new HashMap<>();
        for (Integer parentId : topIds) {
            List<Comment> childList = repliesByParent.getOrDefault(parentId, List.of());
            List<Comment> preview = childList.size() > PREVIEW_REPLIES_LIMIT
                    ? childList.subList(0, PREVIEW_REPLIES_LIMIT)
                    : childList;
            previewRepliesByParent.put(parentId, preview);
            previewRepliesFlat.addAll(preview);
        }

        List<Comment> allVisibleComments = new ArrayList<>(topComments.size() + previewRepliesFlat.size());
        allVisibleComments.addAll(topComments);
        allVisibleComments.addAll(previewRepliesFlat);

        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfilesForComments(allVisibleComments);
        Set<Integer> reactedCommentIds = loadReactedCommentIds(currentUserId, allVisibleComments);

        List<CommentResponse> items = new ArrayList<>(topComments.size());
        for (Comment top : topComments) {
            List<CommentResponse> previewDtos = previewRepliesByParent.getOrDefault(top.getId(), List.of()).stream()
                    .map(reply -> toResponse(reply, artistProfiles, reactedCommentIds, false, null))
                    .toList();

            items.add(toResponse(
                    top,
                    artistProfiles,
                    reactedCommentIds,
                    parentsWithArtistReply.contains(top.getId()),
                    previewDtos
            ));
        }

        return PageResponse.<CommentResponse>builder()
                .items(items)
                .page(topPage.getNumber())
                .size(topPage.getSize())
                .totalElements(topPage.getTotalElements())
                .totalPages(topPage.getTotalPages())
                .last(topPage.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getCommentReplies(
            Integer parentCommentId,
            Integer currentUserId,
            Pageable pageable
    ) {
        Comment parent = commentRepository.findWithDetailsById(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + parentCommentId));
        if (parent.getParent() != null) {
            throw new BadRequestException("Replies can only be loaded for top-level comments");
        }
        if (parent.getPost() == null || !PostStatus.PUBLISHED.name().equalsIgnoreCase(parent.getPost().getStatus())) {
            throw new ResourceNotFoundException("Associated post is not available");
        }

        Page<Comment> replyPage = commentRepository.findRepliesArtistFirst(parentCommentId, pageable);
        List<Comment> replies = replyPage.getContent();

        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfilesForComments(replies);
        Set<Integer> reactedCommentIds = loadReactedCommentIds(currentUserId, replies);

        List<CommentResponse> items = replies.stream()
                .map(reply -> toResponse(reply, artistProfiles, reactedCommentIds, false, null))
                .toList();

        return PageResponse.<CommentResponse>builder()
                .items(items)
                .page(replyPage.getNumber())
                .size(replyPage.getSize())
                .totalElements(replyPage.getTotalElements())
                .totalPages(replyPage.getTotalPages())
                .last(replyPage.isLast())
                .build();
    }

    @Transactional
    public CommentResponse createComment(
            Integer postId,
            Integer userId,
            boolean isAdmin,
            CreateCommentRequest request
    ) {
        Post post = postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            throw new BadRequestException("Cannot comment on a post that is not published");
        }
        if (post.getFandom() == null || !FandomStatus.ACTIVE.name().equalsIgnoreCase(post.getFandom().getStatus())) {
            throw new BadRequestException("Cannot comment in a fandom that is not active");
        }

        Integer fandomId = post.getFandom().getId();
        if (!isAdmin && !fandomMemberRepository.existsByFandom_IdAndUser_Id(fandomId, userId)) {
            throw new ForbiddenException("You must join this fandom before commenting");
        }

        Comment parentComment = null;
        if (request.parentId() != null) {
            parentComment = commentRepository.findWithDetailsById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent comment not found with id: " + request.parentId()));
            if (parentComment.getPost() == null || !parentComment.getPost().getId().equals(postId)) {
                throw new BadRequestException("Parent comment does not belong to this post");
            }
            if (parentComment.getParent() != null) {
                throw new BadRequestException("Only 2-level nested comments are supported; please reply to the top-level comment");
            }
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Comment comment = Comment.builder()
                .parent(parentComment)
                .post(post)
                .user(author)
                .content(request.content().trim())
                .totalReactions(0)
                .totalReplies(0)
                .build();

        Comment saved = commentRepository.save(comment);
        postRepository.incrementTotalComments(postId, 1);
        if (parentComment != null) {
            commentRepository.incrementTotalReplies(parentComment.getId());
        }

        ArtistProfile artistProfile = isArtistUser(author)
                ? artistProfileRepository.findWithDetailsByUserId(author.getId()).orElse(null)
                : null;

        return CommentResponse.from(
                saved,
                PostAuthorResponse.from(author, artistProfile),
                false,
                false,
                parentComment == null ? List.of() : null
        );
    }

    @Transactional
    public CommentResponse updateComment(Integer commentId, Integer userId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findWithDetailsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (comment.getPost() == null || !PostStatus.PUBLISHED.name().equalsIgnoreCase(comment.getPost().getStatus())) {
            throw new BadRequestException("Cannot edit a comment on a post that is not published");
        }
        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own comments");
        }

        comment.setContent(request.content().trim());
        Comment saved = commentRepository.save(comment);

        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfilesForComments(List.of(saved));
        Set<Integer> reactedIds = loadReactedCommentIds(userId, List.of(saved));
        return toResponse(saved, artistProfiles, reactedIds, false, null);
    }

    @Transactional
    public void deleteComment(Integer commentId, Integer userId, boolean isAdmin) {
        Comment comment = commentRepository.findWithDetailsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));

        Post post = comment.getPost();
        boolean isCommentAuthor = comment.getUser() != null && comment.getUser().getId().equals(userId);
        boolean isPostAuthor = post != null && post.getUser() != null && post.getUser().getId().equals(userId);
        boolean isModerator = post != null && post.getFandom() != null
                && isFandomModerator(post.getFandom().getId(), userId);

        if (!isCommentAuthor && !isPostAuthor && !isModerator && !isAdmin) {
            throw new ForbiddenException("You do not have permission to delete this comment");
        }

        if (comment.getParent() != null) {
            // Deleting a reply
            Integer parentId = comment.getParent().getId();
            reactionRepository.deleteByTargetTypeAndTargetIdIn(ReactionTargetType.COMMENT.name(), List.of(commentId));
            commentRepository.delete(comment);
            commentRepository.decrementTotalReplies(parentId);
            if (post != null) {
                postRepository.decrementTotalComments(post.getId(), 1);
            }
        } else {
            // Deleting a top-level comment + all its replies cleanly
            List<Integer> replyIds = commentRepository.findReplyIdsByParentId(commentId);
            List<Integer> allDeletedIds = new ArrayList<>(replyIds.size() + 1);
            allDeletedIds.add(commentId);
            allDeletedIds.addAll(replyIds);

            reactionRepository.deleteByTargetTypeAndTargetIdIn(ReactionTargetType.COMMENT.name(), allDeletedIds);
            if (!replyIds.isEmpty()) {
                commentRepository.deleteAllById(replyIds);
            }
            commentRepository.delete(comment);

            if (post != null) {
                postRepository.decrementTotalComments(post.getId(), allDeletedIds.size());
            }
        }
    }

    private CommentResponse toResponse(
            Comment comment,
            Map<Integer, ArtistProfile> artistProfiles,
            Set<Integer> reactedCommentIds,
            boolean artistReplied,
            List<CommentResponse> previewReplies
    ) {
        User author = comment.getUser();
        ArtistProfile artistProfile = author != null ? artistProfiles.get(author.getId()) : null;
        return CommentResponse.from(
                comment,
                PostAuthorResponse.from(author, artistProfile),
                reactedCommentIds.contains(comment.getId()),
                artistReplied,
                previewReplies
        );
    }

    private Map<Integer, ArtistProfile> loadArtistProfilesForComments(List<Comment> comments) {
        Set<Integer> artistUserIds = new HashSet<>();
        for (Comment c : comments) {
            if (isArtistUser(c.getUser())) {
                artistUserIds.add(c.getUser().getId());
            }
        }
        if (artistUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ArtistProfile> map = new HashMap<>();
        for (ArtistProfile profile : artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(artistUserIds)) {
            map.put(profile.getUserId(), profile);
        }
        return map;
    }

    private Set<Integer> loadReactedCommentIds(Integer currentUserId, List<Comment> comments) {
        if (currentUserId == null || comments.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> ids = comments.stream().map(Comment::getId).toList();
        return new HashSet<>(reactionRepository.findReactedTargetIdsByUser(
                currentUserId, ReactionTargetType.COMMENT.name(), ids
        ));
    }

    private boolean resolveTopLevelSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "LATEST".equalsIgnoreCase(sortBy.trim())) {
            return false;
        }
        if ("POPULAR".equalsIgnoreCase(sortBy.trim()) || "HOT".equalsIgnoreCase(sortBy.trim())) {
            return true;
        }
        throw new BadRequestException("Invalid comment sortBy: " + sortBy + ". Allowed values: latest, popular");
    }

    private boolean isArtistUser(User user) {
        return user != null && user.getRole() != null
                && RoleName.ARTIST.name().equalsIgnoreCase(user.getRole().getName());
    }

    private boolean isFandomModerator(Integer fandomId, Integer userId) {
        return fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, userId)
                .map(FandomMember::getRole)
                .filter(role -> FandomMemberRole.MODERATOR.name().equalsIgnoreCase(role))
                .isPresent();
    }
}
