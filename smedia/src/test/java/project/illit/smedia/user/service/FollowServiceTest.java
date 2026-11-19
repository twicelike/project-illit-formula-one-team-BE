package project.illit.smedia.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.user.dto.FollowActionResponse;
import project.illit.smedia.user.dto.FollowStatusResponse;
import project.illit.smedia.user.entity.Follow;
import project.illit.smedia.user.entity.FollowId;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.FollowRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    private FollowRepository followRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private FollowService followService;
    private User fan;
    private User yunah;

    @BeforeEach
    void setUp() {
        followService = new FollowService(followRepository, userRepository, artistProfileRepository);
        fan = User.builder().id(5).name("Minh Nguyen").totalFollowing(3).totalFollower(1).isActive(true).build();
        yunah = User.builder().id(2).name("Noh Yunah").totalFollowing(1).totalFollower(3).isActive(true).build();
    }

    @Test
    void followAndUnfollow_updatesUserAndArtistCounters() {
        assertThatThrownBy(() -> followService.followUser(5, 5))
                .isInstanceOf(BadRequestException.class);

        when(userRepository.findById(5)).thenReturn(Optional.of(fan));
        when(userRepository.findById(2)).thenReturn(Optional.of(yunah));
        when(followRepository.existsById(new FollowId(5, 2))).thenReturn(false);

        FollowActionResponse followed = followService.followUser(5, 2);
        assertThat(followed.following()).isTrue();
        assertThat(followed.targetTotalFollower()).isEqualTo(4);
        assertThat(followed.myTotalFollowing()).isEqualTo(4);
        verify(followRepository).save(any(Follow.class));
        verify(userRepository).incrementTotalFollowing(5);
        verify(userRepository).incrementTotalFollower(2);
        verify(artistProfileRepository).incrementFollowersCount(2);

        // Duplicate follow rejected
        when(followRepository.existsById(new FollowId(5, 2))).thenReturn(true);
        assertThatThrownBy(() -> followService.followUser(5, 2))
                .isInstanceOf(ConflictException.class);

        // Unfollow syncs counters down
        Follow existing = Follow.builder().id(new FollowId(5, 2)).follower(fan).following(yunah).build();
        when(followRepository.findById(new FollowId(5, 2))).thenReturn(Optional.of(existing));

        FollowActionResponse unfollowed = followService.unfollowUser(5, 2);
        assertThat(unfollowed.following()).isFalse();
        assertThat(unfollowed.targetTotalFollower()).isEqualTo(2);
        assertThat(unfollowed.myTotalFollowing()).isEqualTo(2);
        verify(followRepository).delete(existing);
        verify(userRepository).decrementTotalFollowing(5);
        verify(userRepository).decrementTotalFollower(2);
        verify(artistProfileRepository).decrementFollowersCount(2);
    }

    @Test
    void getFollowStatus_reportsMutualCorrectly() {
        when(userRepository.findById(2)).thenReturn(Optional.of(yunah));
        when(followRepository.existsById(new FollowId(5, 2))).thenReturn(true);
        when(followRepository.existsById(new FollowId(2, 5))).thenReturn(true);

        FollowStatusResponse status = followService.getFollowStatus(5, 2);
        assertThat(status.following()).isTrue();
        assertThat(status.followedBy()).isTrue();
        assertThat(status.mutual()).isTrue();
    }
}
