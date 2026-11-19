package project.illit.smedia.interaction.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.interaction.dto.ReactionToggleResponse;
import project.illit.smedia.interaction.dto.ReactorResponse;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.interaction.entity.Reaction;
import project.illit.smedia.interaction.entity.ReactionTargetType;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.entity.PostStatus;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public ReactionService(
            ReactionRepository reactionRepository,
            PostRepository postRepository,
            CommentRepository commentRepository,
            UserRepository userRepository,
            ArtistProfileRepository artistProfileRepository
    ) {
        this.reactionRepository = reactionRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional
    public ReactionToggleResponse togglePostReaction(Integer postId, Integer userId) {
        Post post = postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        ensurePostInteractable(post);

        String targetType = ReactionTargetType.POST.name();
        Optional<Reaction> existing = reactionRepository.findByTargetIdAndTargetTypeAndUser_Id(postId, targetType, userId);
        int currentCount = post.getTotalReactions() != null ? post.getTotalReactions() : 0;

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            postRepository.decrementTotalReactions(postId);
            return new ReactionToggleResponse(postId, targetType, false, Math.max(0, currentCount - 1));
        }

        User user = requireUser(userId);
        saveReactionSafely(postId, targetType, user);
        postRepository.incrementTotalReactions(postId);
        return new ReactionToggleResponse(postId, targetType, true, currentCount + 1);
    }

    @Transactional
    public ReactionToggleResponse toggleCommentReaction(Integer commentId, Integer userId) {
        Comment comment = commentRepository.findWithDetailsById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        if (comment.getPost() == null) {
            throw new ResourceNotFoundException("Associated post not found for comment: " + commentId);
        }
        ensurePostInteractable(comment.getPost());

        String targetType = ReactionTargetType.COMMENT.name();
        Optional<Reaction> existing = reactionRepository.findByTargetIdAndTargetTypeAndUser_Id(commentId, targetType, userId);
        int currentCount = comment.getTotalReactions() != null ? comment.getTotalReactions() : 0;

        if (existing.isPresent()) {
            reactionRepository.delete(existing.get());
            commentRepository.decrementTotalReactions(commentId);
            return new ReactionToggleResponse(commentId, targetType, false, Math.max(0, currentCount - 1));
        }

        User user = requireUser(userId);
        saveReactionSafely(commentId, targetType, user);
        commentRepository.incrementTotalReactions(commentId);
        return new ReactionToggleResponse(commentId, targetType, true, currentCount + 1);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReactorResponse> getPostReactors(Integer postId, Pageable pageable) {
        Post post = postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        Page<Reaction> page = reactionRepository.findByTargetIdAndTargetTypeOrderByIdDesc(
                postId, ReactionTargetType.POST.name(), pageable
        );

        Set<Integer> artistUserIds = new HashSet<>();
        for (Reaction r : page.getContent()) {
            User u = r.getUser();
            if (u != null && u.getRole() != null && RoleName.ARTIST.name().equalsIgnoreCase(u.getRole().getName())) {
                artistUserIds.add(u.getId());
            }
        }
        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfiles(artistUserIds);

        Page<ReactorResponse> mapped = page.map(r -> {
            User u = r.getUser();
            return ReactorResponse.from(u, artistProfiles.get(u.getId()));
        });
        return PageResponse.from(mapped);
    }

    private void ensurePostInteractable(Post post) {
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            throw new BadRequestException("Cannot react to a post that is not published");
        }
        if (post.getFandom() != null && !FandomStatus.ACTIVE.name().equalsIgnoreCase(post.getFandom().getStatus())) {
            throw new BadRequestException("Cannot react in a fandom that is not active");
        }
    }

    private void saveReactionSafely(Integer targetId, String targetType, User user) {
        try {
            reactionRepository.save(Reaction.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .user(user)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Reaction was already processed concurrently, please retry");
        }
    }

    private User requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Map<Integer, ArtistProfile> loadArtistProfiles(Set<Integer> artistUserIds) {
        if (artistUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ArtistProfile> map = new HashMap<>();
        for (ArtistProfile profile : artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(artistUserIds)) {
            map.put(profile.getUserId(), profile);
        }
        return map;
    }
}
