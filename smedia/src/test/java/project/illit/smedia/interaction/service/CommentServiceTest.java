package project.illit.smedia.interaction.service;

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
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.interaction.dto.CommentResponse;
import project.illit.smedia.interaction.dto.CreateCommentRequest;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.entity.Post;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private FandomMemberRepository fandomMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private CommentService commentService;
    private OfficialFandom illitFandom;
    private User fan;
    private User yunah;
    private ArtistProfile yunahProfile;
    private Post post;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(
                commentRepository,
                postRepository,
                reactionRepository,
                fandomMemberRepository,
                userRepository,
                artistProfileRepository
        );
        illitFandom = OfficialFandom.builder().id(1).name("ILLIT").status("ACTIVE").build();
        fan = User.builder().id(5).name("Minh Nguyen").role(Role.builder().id(3).name("FAN").build()).build();
        yunah = User.builder().id(2).name("Noh Yunah").role(Role.builder().id(2).name("ARTIST").build()).build();
        yunahProfile = ArtistProfile.builder().userId(2).user(yunah).stageName("YUNAH").build();
        post = Post.builder().id(10).fandom(illitFandom).user(fan).status("PUBLISHED").totalComments(2).build();
    }

    @Test
    void getPostComments_marksArtistRepliesAndLoadsPreviewReplies() {
        Comment fanComment = Comment.builder()
                .id(1)
                .post(post)
                .user(fan)
                .content("Chị Yunah ăn tối chưa?")
                .totalReplies(1)
                .createdAt(LocalDateTime.now())
                .build();
        Comment artistReply = Comment.builder()
                .id(2)
                .parent(fanComment)
                .post(post)
                .user(yunah)
                .content("Chị ăn rồi nè!")
                .createdAt(LocalDateTime.now())
                .build();

        PageRequest pageable = PageRequest.of(0, 20);
        Page<Comment> topPage = new PageImpl<>(List.of(fanComment), pageable, 1);

        when(postRepository.findWithDetailsById(10)).thenReturn(Optional.of(post));
        when(commentRepository.findTopLevelCommentsArtistFirstLatest(10, pageable)).thenReturn(topPage);
        when(commentRepository.findRepliesByParentIdsArtistFirst(List.of(1))).thenReturn(List.of(artistReply));
        when(artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(Set.of(2)))
                .thenReturn(List.of(yunahProfile));
        when(reactionRepository.findReactedTargetIdsByUser(5, "COMMENT", List.of(1, 2)))
                .thenReturn(List.of(2));

        PageResponse<CommentResponse> result = commentService.getPostComments(10, "latest", 5, pageable);

        assertThat(result.getItems()).hasSize(1);
        CommentResponse topDto = result.getItems().get(0);
        assertThat(topDto.artistComment()).isFalse();
        assertThat(topDto.artistReplied()).isTrue();
        assertThat(topDto.previewReplies()).hasSize(1);
        assertThat(topDto.previewReplies().get(0).artistComment()).isTrue();
        assertThat(topDto.previewReplies().get(0).author().stageName()).isEqualTo("YUNAH");
        assertThat(topDto.previewReplies().get(0).reactedByMe()).isTrue();
    }

    @Test
    void createComment_enforcesMembershipAndTwoLevelLimit() {
        when(postRepository.findWithDetailsById(10)).thenReturn(Optional.of(post));
        when(fandomMemberRepository.existsByFandom_IdAndUser_Id(1, 99)).thenReturn(false);

        assertThatThrownBy(() -> commentService.createComment(10, 99, false, new CreateCommentRequest("Hi", null)))
                .isInstanceOf(ForbiddenException.class);

        // Replying to a reply (level 3) must be rejected
        Comment top = Comment.builder().id(1).post(post).build();
        Comment reply = Comment.builder().id(2).parent(top).post(post).build();
        when(fandomMemberRepository.existsByFandom_IdAndUser_Id(1, 5)).thenReturn(true);
        when(commentRepository.findWithDetailsById(2)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> commentService.createComment(10, 5, false, new CreateCommentRequest("Level 3", 2)))
                .isInstanceOf(BadRequestException.class);

        // Valid level-2 reply increments both post.totalComments and parent.totalReplies
        when(commentRepository.findWithDetailsById(1)).thenReturn(Optional.of(top));
        when(userRepository.findById(2)).thenReturn(Optional.of(yunah));
        when(fandomMemberRepository.existsByFandom_IdAndUser_Id(1, 2)).thenReturn(true);
        when(artistProfileRepository.findWithDetailsByUserId(2)).thenReturn(Optional.of(yunahProfile));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(50);
            return c;
        });

        CommentResponse created = commentService.createComment(10, 2, false, new CreateCommentRequest("Reply!", 1));
        assertThat(created.id()).isEqualTo(50);
        assertThat(created.parentId()).isEqualTo(1);
        assertThat(created.artistComment()).isTrue();
        verify(postRepository).incrementTotalComments(10, 1);
        verify(commentRepository).incrementTotalReplies(1);
    }

    @Test
    void deleteTopLevelComment_removesChildRepliesAndDecrementsTotalCountAccurately() {
        Comment top = Comment.builder().id(1).post(post).user(fan).build();
        when(commentRepository.findWithDetailsById(1)).thenReturn(Optional.of(top));
        when(commentRepository.findReplyIdsByParentId(1)).thenReturn(List.of(2, 3));

        commentService.deleteComment(1, 5, false);

        verify(reactionRepository).deleteByTargetTypeAndTargetIdIn("COMMENT", List.of(1, 2, 3));
        verify(commentRepository).deleteAllById(List.of(2, 3));
        verify(commentRepository).delete(top);
        verify(postRepository).decrementTotalComments(10, 3);
    }
}
