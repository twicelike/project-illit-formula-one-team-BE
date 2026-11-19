package project.illit.smedia.interaction.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.interaction.dto.ReactionToggleResponse;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.interaction.entity.Reaction;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.interaction.repository.ReactionRepository;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactionServiceTest {

    @Mock
    private ReactionRepository reactionRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private ReactionService reactionService;
    private OfficialFandom illitFandom;
    private User fan;

    @BeforeEach
    void setUp() {
        reactionService = new ReactionService(
                reactionRepository,
                postRepository,
                commentRepository,
                userRepository,
                artistProfileRepository
        );
        illitFandom = OfficialFandom.builder().id(1).name("ILLIT").status("ACTIVE").build();
        fan = User.builder().id(5).name("Minh Nguyen").build();
    }

    @Test
    void togglePostReaction_likesAndUnlikesAndUpdatesCounter() {
        Post post = Post.builder().id(10).fandom(illitFandom).status("PUBLISHED").totalReactions(3).build();
        when(postRepository.findWithDetailsById(10)).thenReturn(Optional.of(post));

        // Like
        when(reactionRepository.findByTargetIdAndTargetTypeAndUser_Id(10, "POST", 5)).thenReturn(Optional.empty());
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));

        ReactionToggleResponse liked = reactionService.togglePostReaction(10, 5);
        assertThat(liked.reacted()).isTrue();
        assertThat(liked.totalReactions()).isEqualTo(4);
        verify(reactionRepository).save(any(Reaction.class));
        verify(postRepository).incrementTotalReactions(10);

        // Unlike
        Reaction existing = Reaction.builder().id(100).targetId(10).targetType("POST").user(fan).build();
        when(reactionRepository.findByTargetIdAndTargetTypeAndUser_Id(10, "POST", 5)).thenReturn(Optional.of(existing));

        ReactionToggleResponse unliked = reactionService.togglePostReaction(10, 5);
        assertThat(unliked.reacted()).isFalse();
        assertThat(unliked.totalReactions()).isEqualTo(2);
        verify(reactionRepository).delete(existing);
        verify(postRepository).decrementTotalReactions(10);
    }

    @Test
    void toggleCommentReaction_likesAndRejectsHiddenPost() {
        Post publishedPost = Post.builder().id(10).fandom(illitFandom).status("PUBLISHED").build();
        Comment comment = Comment.builder().id(20).post(publishedPost).totalReactions(1).build();

        when(commentRepository.findWithDetailsById(20)).thenReturn(Optional.of(comment));
        when(reactionRepository.findByTargetIdAndTargetTypeAndUser_Id(20, "COMMENT", 5)).thenReturn(Optional.empty());
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));

        ReactionToggleResponse res = reactionService.toggleCommentReaction(20, 5);
        assertThat(res.reacted()).isTrue();
        assertThat(res.totalReactions()).isEqualTo(2);
        verify(commentRepository).incrementTotalReactions(20);

        // Hidden post must reject reaction
        Post hiddenPost = Post.builder().id(11).fandom(illitFandom).status("HIDDEN").build();
        when(postRepository.findWithDetailsById(11)).thenReturn(Optional.of(hiddenPost));
        assertThatThrownBy(() -> reactionService.togglePostReaction(11, 5))
                .isInstanceOf(BadRequestException.class);
    }
}
