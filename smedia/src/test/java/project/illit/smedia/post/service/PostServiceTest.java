package project.illit.smedia.post.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ForbiddenException;
import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.dto.CreatePostRequest;
import project.illit.smedia.post.dto.MediaItemRequest;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.post.dto.UpdatePostRequest;
import project.illit.smedia.post.entity.Media;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.repository.MediaRepository;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private FandomService fandomService;
    @Mock
    private FandomMemberRepository fandomMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReactionRepository reactionRepository;

    private PostService postService;
    private OfficialFandom illitFandom;
    private User fanUser;
    private User yunahUser;
    private ArtistProfile yunahProfile;

    @BeforeEach
    void setUp() {
        postService = new PostService(
                postRepository,
                mediaRepository,
                fandomService,
                fandomMemberRepository,
                userRepository,
                artistProfileRepository,
                commentRepository,
                reactionRepository
        );
        illitFandom = OfficialFandom.builder().id(1).name("ILLIT").status("ACTIVE").build();
        fanUser = User.builder()
                .id(5)
                .name("Minh Nguyen")
                .role(Role.builder().id(3).name("FAN").build())
                .isActive(true)
                .build();
        yunahUser = User.builder()
                .id(2)
                .name("Noh Yunah")
                .avatar("https://cdn.example.com/yunah.jpg")
                .role(Role.builder().id(2).name("ARTIST").build())
                .isActive(true)
                .build();
        yunahProfile = ArtistProfile.builder()
                .userId(2)
                .user(yunahUser)
                .stageName("YUNAH")
                .avatar("https://cdn.example.com/yunah-stage.jpg")
                .build();
    }

    @Test
    void createPost_rejectsNonMemberAndEmptyPost() {
        when(fandomService.requireFandom(1)).thenReturn(illitFandom);

        assertThatThrownBy(() -> postService.createPost(1, 5, false, new CreatePostRequest("   ", List.of())))
                .isInstanceOf(BadRequestException.class);

        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 5)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.createPost(1, 5, false, new CreatePostRequest("Hello GLLIT!", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void createPost_setsArtistPostTrueForOfficialArtistAndAttachesMedia() {
        FandomMember artistMembership = FandomMember.builder().id(1L).fandom(illitFandom).user(yunahUser).role("ARTIST").build();
        when(fandomService.requireFandom(1)).thenReturn(illitFandom);
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 2)).thenReturn(Optional.of(artistMembership));
        when(userRepository.findById(2)).thenReturn(Optional.of(yunahUser));
        when(artistProfileRepository.findWithDetailsByUserId(2)).thenReturn(Optional.of(yunahProfile));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(101);
            return p;
        });
        when(mediaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CreatePostRequest req = new CreatePostRequest(
                "Selfie hậu trường cho GLLIT!",
                List.of(new MediaItemRequest("https://cdn.example.com/p1.jpg", null))
        );

        PostResponse created = postService.createPost(1, 2, false, req);

        assertThat(created.id()).isEqualTo(101);
        assertThat(created.artistPost()).isTrue();
        assertThat(created.author().stageName()).isEqualTo("YUNAH");
        assertThat(created.author().verified()).isTrue();
        assertThat(created.media()).hasSize(1);
        assertThat(created.media().get(0).thumbnail()).isEqualTo("https://cdn.example.com/p1.jpg");
    }

    @Test
    void getFandomFeed_enrichesMediaAndArtistHighlightBadges() {
        Post fanPost = Post.builder()
                .id(3)
                .user(fanUser)
                .fandom(illitFandom)
                .content("Mới unbox album ILLIT!")
                .totalReactions(2)
                .totalComments(2)
                .isArtistPost(false)
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(fanPost), pageable, 1);

        Media media = Media.builder()
                .id(30)
                .targetId(3)
                .targetType("POST")
                .mediaUrl("https://cdn.example.com/album.jpg")
                .thumbnail("https://cdn.example.com/album-thumb.jpg")
                .status("ACTIVE")
                .build();

        when(fandomService.requireFandom(1)).thenReturn(illitFandom);
        when(postRepository.findFandomFeedHot(1, "PUBLISHED", true, false, true, pageable)).thenReturn(page);
        when(mediaRepository.findByTargetTypeAndTargetIdInAndStatusIgnoreCaseOrderByIdAsc("POST", List.of(3), "ACTIVE"))
                .thenReturn(List.of(media));
        when(commentRepository.findDistinctArtistCommentersByPostIds(List.of(3)))
                .thenReturn(List.<Object[]>of(new Object[]{3, 2}));
        when(reactionRepository.findDistinctArtistReactorsByTargetIds("POST", List.of(3)))
                .thenReturn(List.<Object[]>of(new Object[]{3, 2}));
        when(artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(Set.of(2)))
                .thenReturn(List.of(yunahProfile));
        when(reactionRepository.findReactedTargetIdsByUser(5, "POST", List.of(3)))
                .thenReturn(List.of(3));

        PageResponse<PostResponse> feed = postService.getFandomFeed(1, "FAN", "hot", true, 5, false, pageable);

        assertThat(feed.getItems()).hasSize(1);
        PostResponse item = feed.getItems().get(0);
        assertThat(item.reactedByMe()).isTrue();
        assertThat(item.media()).hasSize(1);
        assertThat(item.artistInteractions().artistReplied()).isTrue();
        assertThat(item.artistInteractions().artistLiked()).isTrue();
        assertThat(item.artistInteractions().highlightText()).isEqualTo("YUNAH đã bình luận");
    }

    @Test
    void updateAndDeletePost_enforcesOwnershipAndModeratorRights() {
        Post fanPost = Post.builder()
                .id(3)
                .user(fanUser)
                .fandom(illitFandom)
                .content("Old content")
                .status("PUBLISHED")
                .build();

        when(postRepository.findWithDetailsById(3)).thenReturn(Optional.of(fanPost));

        // Another user cannot edit
        assertThatThrownBy(() -> postService.updatePost(3, 6, new UpdatePostRequest("Hacked", null)))
                .isInstanceOf(ForbiddenException.class);

        // Stranger cannot delete
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 6)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> postService.deletePost(3, 6, false))
                .isInstanceOf(ForbiddenException.class);

        // Fandom moderator can soft-delete
        FandomMember moderator = FandomMember.builder().id(77L).fandom(illitFandom).role("MODERATOR").build();
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 9)).thenReturn(Optional.of(moderator));

        postService.deletePost(3, 9, false);

        assertThat(fanPost.getStatus()).isEqualTo("DELETED");
        verify(mediaRepository).softDeleteActiveByTarget("POST", 3);
    }
}
