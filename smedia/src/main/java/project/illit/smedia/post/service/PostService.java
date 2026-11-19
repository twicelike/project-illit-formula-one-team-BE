package project.illit.smedia.post.service;

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
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.dto.ArtistBadgeActor;
import project.illit.smedia.post.dto.ArtistInteractionSummary;
import project.illit.smedia.post.dto.CreatePostRequest;
import project.illit.smedia.post.dto.FandomMediaGalleryItem;
import project.illit.smedia.post.dto.MediaItemRequest;
import project.illit.smedia.post.dto.MediaResponse;
import project.illit.smedia.post.dto.PostAuthorResponse;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.post.dto.UpdatePostRequest;
import project.illit.smedia.post.entity.Media;
import project.illit.smedia.post.entity.MediaStatus;
import project.illit.smedia.post.entity.MediaTargetType;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.entity.PostStatus;
import project.illit.smedia.post.repository.MediaRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;
    private final FandomService fandomService;
    private final FandomMemberRepository fandomMemberRepository;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    public PostService(
            PostRepository postRepository,
            MediaRepository mediaRepository,
            FandomService fandomService,
            FandomMemberRepository fandomMemberRepository,
            UserRepository userRepository,
            ArtistProfileRepository artistProfileRepository,
            CommentRepository commentRepository,
            ReactionRepository reactionRepository
    ) {
        this.postRepository = postRepository;
        this.mediaRepository = mediaRepository;
        this.fandomService = fandomService;
        this.fandomMemberRepository = fandomMemberRepository;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.commentRepository = commentRepository;
        this.reactionRepository = reactionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getFandomFeed(
            Integer fandomId,
            String tab,
            String sortBy,
            boolean hasMedia,
            Integer currentUserId,
            boolean isAdmin,
            Pageable pageable
    ) {
        OfficialFandom fandom = fandomService.requireFandom(fandomId);
        if (!isAdmin && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }

        FeedTab parsedTab = FeedTab.fromString(tab);
        FeedSort parsedSort = FeedSort.fromString(sortBy);

        Page<Post> page = switch (parsedSort) {
            case LATEST -> postRepository.findFandomFeedLatest(
                    fandomId,
                    PostStatus.PUBLISHED.name(),
                    parsedTab.filterByArtist,
                    parsedTab.artistPostValue,
                    hasMedia,
                    pageable
            );
            case HOT -> postRepository.findFandomFeedHot(
                    fandomId,
                    PostStatus.PUBLISHED.name(),
                    parsedTab.filterByArtist,
                    parsedTab.artistPostValue,
                    hasMedia,
                    pageable
            );
        };

        return enrichPostPage(page, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getUserPosts(Integer targetUserId, Integer currentUserId, Pageable pageable) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));
        if (!Boolean.TRUE.equals(target.getIsActive())) {
            throw new ResourceNotFoundException("User not found with id: " + targetUserId);
        }

        Page<Post> page = postRepository.findByUser_IdAndStatusIgnoreCaseOrderByCreatedAtDesc(
                targetUserId,
                PostStatus.PUBLISHED.name(),
                pageable
        );
        return enrichPostPage(page, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getFollowingFeed(
            Integer viewerId,
            boolean artistOnly,
            boolean hasMedia,
            Pageable pageable
    ) {
        Page<Post> page = postRepository.findFollowingFeed(viewerId, artistOnly, hasMedia, pageable);
        return enrichPostPage(page, viewerId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FandomMediaGalleryItem> getFandomMediaGallery(
            Integer fandomId,
            boolean artistOnly,
            boolean isAdmin,
            Pageable pageable
    ) {
        OfficialFandom fandom = fandomService.requireFandom(fandomId);
        if (!isAdmin && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }

        Page<Object[]> rawPage = mediaRepository.findFandomPostGallery(fandomId, artistOnly, pageable);
        Page<FandomMediaGalleryItem> mapped = rawPage.map(row -> {
            Media media = (Media) row[0];
            Integer postId = (Integer) row[1];
            String authorName = (String) row[2];
            Boolean artistPost = (Boolean) row[3];
            return new FandomMediaGalleryItem(
                    media.getId(),
                    postId,
                    media.getMediaUrl(),
                    media.getThumbnail(),
                    authorName,
                    Boolean.TRUE.equals(artistPost),
                    media.getCreatedAt()
            );
        });
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostDetail(Integer postId, Integer currentUserId, boolean isAdmin) {
        Post post = requirePost(postId);
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            boolean canViewHidden = isAdmin || isFandomModerator(post.getFandom().getId(), currentUserId);
            if (!canViewHidden) {
                throw new ResourceNotFoundException("Post not found with id: " + postId);
            }
        }
        return enrichSinglePost(post, currentUserId);
    }

    @Transactional
    public PostResponse createPost(Integer fandomId, Integer userId, boolean isAdmin, CreatePostRequest request) {
        OfficialFandom fandom = fandomService.requireFandom(fandomId);
        if (!FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new BadRequestException("Cannot post in a fandom that is not active");
        }

        String normalizedContent = normalizeContent(request.content());
        List<MediaItemRequest> mediaItems = request.media() != null ? request.media() : List.of();
        if (normalizedContent == null && mediaItems.isEmpty()) {
            throw new BadRequestException("Post must contain either text content or at least one media item");
        }

        Optional<FandomMember> membershipOpt = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, userId);
        if (!isAdmin && membershipOpt.isEmpty()) {
            throw new ForbiddenException("You must join this fandom before creating a post");
        }

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isOfficialArtistInFandom = isOfficialArtist(author, membershipOpt.orElse(null));

        Post post = Post.builder()
                .user(author)
                .fandom(fandom)
                .content(normalizedContent)
                .totalReactions(0)
                .totalComments(0)
                .isArtistPost(isOfficialArtistInFandom)
                .status(PostStatus.PUBLISHED.name())
                .build();

        Post savedPost = postRepository.save(post);
        List<Media> savedMedia = saveMediaItems(savedPost.getId(), mediaItems);

        ArtistProfile authorArtistProfile = isOfficialArtistInFandom
                ? artistProfileRepository.findWithDetailsByUserId(userId).orElse(null)
                : null;

        return PostResponse.from(
                savedPost,
                PostAuthorResponse.from(author, authorArtistProfile),
                savedMedia.stream().map(MediaResponse::from).toList(),
                false,
                ArtistInteractionSummary.of(List.of(), List.of())
        );
    }

    @Transactional
    public PostResponse updatePost(Integer postId, Integer userId, UpdatePostRequest request) {
        Post post = requirePost(postId);
        if (!PostStatus.PUBLISHED.name().equalsIgnoreCase(post.getStatus())) {
            throw new BadRequestException("Only published posts can be edited");
        }
        if (post.getUser() == null || !post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You can only edit your own posts");
        }

        String normalizedContent = normalizeContent(request.content());
        List<MediaItemRequest> incomingMedia = request.media();

        if (incomingMedia != null) {
            if (normalizedContent == null && incomingMedia.isEmpty()) {
                throw new BadRequestException("Post must contain either text content or at least one media item");
            }
            mediaRepository.softDeleteActiveByTarget(MediaTargetType.POST.name(), postId);
            saveMediaItems(postId, incomingMedia);
        } else {
            List<Media> existingMedia = mediaRepository.findByTargetTypeAndTargetIdAndStatusIgnoreCaseOrderByIdAsc(
                    MediaTargetType.POST.name(), postId, MediaStatus.ACTIVE.name()
            );
            if (normalizedContent == null && existingMedia.isEmpty()) {
                throw new BadRequestException("Post must contain either text content or at least one media item");
            }
        }

        post.setContent(normalizedContent);
        Post updated = postRepository.save(post);
        return enrichSinglePost(updated, userId);
    }

    @Transactional
    public void deletePost(Integer postId, Integer userId, boolean isAdmin) {
        Post post = requirePost(postId);
        if (PostStatus.DELETED.name().equalsIgnoreCase(post.getStatus())) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }

        boolean isAuthor = post.getUser() != null && post.getUser().getId().equals(userId);
        boolean isModerator = post.getFandom() != null && isFandomModerator(post.getFandom().getId(), userId);
        if (!isAuthor && !isAdmin && !isModerator) {
            throw new ForbiddenException("You do not have permission to delete this post");
        }

        post.setStatus(PostStatus.DELETED.name());
        postRepository.save(post);
        mediaRepository.softDeleteActiveByTarget(MediaTargetType.POST.name(), postId);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listPostsForAdmin(Integer fandomId, String status, Pageable pageable) {
        String normalizedStatus = (status != null && !status.isBlank())
                ? PostStatus.fromString(status).name()
                : null;

        Page<Post> page;
        if (fandomId != null && normalizedStatus != null) {
            page = postRepository.findByFandom_IdAndStatusIgnoreCase(fandomId, normalizedStatus, pageable);
        } else if (fandomId != null) {
            page = postRepository.findByFandom_Id(fandomId, pageable);
        } else if (normalizedStatus != null) {
            page = postRepository.findByStatusIgnoreCase(normalizedStatus, pageable);
        } else {
            page = postRepository.findAll(pageable);
        }

        return enrichPostPage(page, null);
    }

    @Transactional
    public PostResponse updatePostStatus(Integer postId, String statusValue) {
        Post post = requirePost(postId);
        PostStatus newStatus = PostStatus.fromString(statusValue);
        post.setStatus(newStatus.name());
        Post updated = postRepository.save(post);

        if (newStatus == PostStatus.DELETED) {
            mediaRepository.softDeleteActiveByTarget(MediaTargetType.POST.name(), postId);
        }

        return enrichSinglePost(updated, null);
    }

    public Post requirePost(Integer postId) {
        return postRepository.findWithDetailsById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
    }

    // ==================== BATCH ENRICHMENT (ZERO N+1 QUERIES) ====================

    private PostResponse enrichSinglePost(Post post, Integer currentUserId) {
        List<PostResponse> enriched = enrichPosts(List.of(post), currentUserId);
        return enriched.get(0);
    }

    private PageResponse<PostResponse> enrichPostPage(Page<Post> page, Integer currentUserId) {
        List<PostResponse> enrichedItems = enrichPosts(page.getContent(), currentUserId);
        return PageResponse.<PostResponse>builder()
                .items(enrichedItems)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private List<PostResponse> enrichPosts(List<Post> posts, Integer currentUserId) {
        if (posts.isEmpty()) {
            return List.of();
        }

        List<Integer> postIds = posts.stream().map(Post::getId).toList();

        // 1. Batch-load active media for all posts
        Map<Integer, List<MediaResponse>> mediaByPostId = loadActiveMediaGrouped(postIds);

        // 2. Batch-load artist commenters and artist reactors for Weverse highlight badges
        Map<Integer, List<Integer>> artistCommenterIdsByPost = groupPairRows(
                commentRepository.findDistinctArtistCommentersByPostIds(postIds)
        );
        Map<Integer, List<Integer>> artistReactorIdsByPost = groupPairRows(
                reactionRepository.findDistinctArtistReactorsByTargetIds(MediaTargetType.POST.name(), postIds)
        );

        // 3. Collect all artist user IDs (authors + commenters + reactors) and load ArtistProfiles in 1 query
        Set<Integer> artistUserIdsToLoad = new HashSet<>();
        for (Post post : posts) {
            User author = post.getUser();
            if (author != null && author.getRole() != null
                    && RoleName.ARTIST.name().equalsIgnoreCase(author.getRole().getName())) {
                artistUserIdsToLoad.add(author.getId());
            }
        }
        artistCommenterIdsByPost.values().forEach(artistUserIdsToLoad::addAll);
        artistReactorIdsByPost.values().forEach(artistUserIdsToLoad::addAll);

        Map<Integer, ArtistProfile> artistProfilesByUserId = loadArtistProfiles(artistUserIdsToLoad);

        // 4. Batch-load current user's reactions
        Set<Integer> reactedPostIds = loadUserReactedPostIds(currentUserId, postIds);

        // 5. Assemble responses
        List<PostResponse> results = new ArrayList<>(posts.size());
        for (Post post : posts) {
            Integer pid = post.getId();
            User author = post.getUser();
            ArtistProfile authorProfile = author != null ? artistProfilesByUserId.get(author.getId()) : null;

            List<ArtistBadgeActor> repliedArtists = resolveBadgeActors(
                    artistCommenterIdsByPost.getOrDefault(pid, List.of()),
                    artistProfilesByUserId
            );
            List<ArtistBadgeActor> likedArtists = resolveBadgeActors(
                    artistReactorIdsByPost.getOrDefault(pid, List.of()),
                    artistProfilesByUserId
            );

            results.add(PostResponse.from(
                    post,
                    PostAuthorResponse.from(author, authorProfile),
                    mediaByPostId.getOrDefault(pid, List.of()),
                    reactedPostIds.contains(pid),
                    ArtistInteractionSummary.of(repliedArtists, likedArtists)
            ));
        }

        return results;
    }

    private Map<Integer, List<MediaResponse>> loadActiveMediaGrouped(List<Integer> postIds) {
        List<Media> mediaList = mediaRepository.findByTargetTypeAndTargetIdInAndStatusIgnoreCaseOrderByIdAsc(
                MediaTargetType.POST.name(),
                postIds,
                MediaStatus.ACTIVE.name()
        );
        Map<Integer, List<MediaResponse>> grouped = new HashMap<>();
        for (Media media : mediaList) {
            grouped.computeIfAbsent(media.getTargetId(), ignored -> new ArrayList<>())
                    .add(MediaResponse.from(media));
        }
        return grouped;
    }

    private Map<Integer, List<Integer>> groupPairRows(List<Object[]> rows) {
        Map<Integer, Set<Integer>> uniqueGrouped = new HashMap<>();
        for (Object[] row : rows) {
            Integer postId = (Integer) row[0];
            Integer artistUserId = (Integer) row[1];
            uniqueGrouped.computeIfAbsent(postId, ignored -> new LinkedHashSet<>()).add(artistUserId);
        }
        Map<Integer, List<Integer>> result = new HashMap<>();
        uniqueGrouped.forEach((k, v) -> result.put(k, new ArrayList<>(v)));
        return result;
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

    private Set<Integer> loadUserReactedPostIds(Integer currentUserId, List<Integer> postIds) {
        if (currentUserId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(reactionRepository.findReactedTargetIdsByUser(
                currentUserId, MediaTargetType.POST.name(), postIds
        ));
    }

    private List<ArtistBadgeActor> resolveBadgeActors(
            List<Integer> artistUserIds,
            Map<Integer, ArtistProfile> artistProfilesByUserId
    ) {
        if (artistUserIds.isEmpty()) {
            return List.of();
        }
        List<ArtistBadgeActor> actors = new ArrayList<>();
        for (Integer artistUserId : artistUserIds) {
            ArtistProfile profile = artistProfilesByUserId.get(artistUserId);
            if (profile != null) {
                actors.add(ArtistBadgeActor.from(profile));
            }
        }
        return actors;
    }

    private List<Media> saveMediaItems(Integer postId, List<MediaItemRequest> mediaItems) {
        if (mediaItems == null || mediaItems.isEmpty()) {
            return List.of();
        }
        List<Media> entities = mediaItems.stream()
                .map(item -> Media.builder()
                        .targetId(postId)
                        .targetType(MediaTargetType.POST.name())
                        .mediaUrl(item.mediaUrl().trim())
                        .thumbnail(item.thumbnail() != null && !item.thumbnail().isBlank()
                                ? item.thumbnail().trim()
                                : item.mediaUrl().trim())
                        .status(MediaStatus.ACTIVE.name())
                        .build())
                .toList();
        return mediaRepository.saveAll(entities);
    }

    private String normalizeContent(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim();
    }

    private boolean isOfficialArtist(User user, FandomMember membership) {
        boolean hasArtistSystemRole = user.getRole() != null
                && RoleName.ARTIST.name().equalsIgnoreCase(user.getRole().getName());
        boolean hasArtistFandomRole = membership != null
                && FandomMemberRole.ARTIST.name().equalsIgnoreCase(membership.getRole());
        return hasArtistSystemRole && hasArtistFandomRole;
    }

    private boolean isFandomModerator(Integer fandomId, Integer userId) {
        if (fandomId == null || userId == null) {
            return false;
        }
        return fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, userId)
                .map(FandomMember::getRole)
                .filter(role -> FandomMemberRole.MODERATOR.name().equalsIgnoreCase(role))
                .isPresent();
    }

    private enum FeedTab {
        ALL(false, false),
        ARTIST(true, true),
        FAN(true, false);

        final boolean filterByArtist;
        final boolean artistPostValue;

        FeedTab(boolean filterByArtist, boolean artistPostValue) {
            this.filterByArtist = filterByArtist;
            this.artistPostValue = artistPostValue;
        }

        static FeedTab fromString(String value) {
            if (value == null || value.isBlank() || "FEED".equalsIgnoreCase(value.trim())) {
                return ALL;
            }
            try {
                return FeedTab.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid feed tab: " + value + ". Allowed values: FEED, ALL, ARTIST, FAN");
            }
        }
    }

    private enum FeedSort {
        LATEST,
        HOT;

        static FeedSort fromString(String value) {
            if (value == null || value.isBlank() || "LATEST".equalsIgnoreCase(value.trim())) {
                return LATEST;
            }
            if ("HOT".equalsIgnoreCase(value.trim()) || "POPULAR".equalsIgnoreCase(value.trim())) {
                return HOT;
            }
            throw new BadRequestException("Invalid sortBy: " + value + ". Allowed values: latest, hot, popular");
        }
    }
}
