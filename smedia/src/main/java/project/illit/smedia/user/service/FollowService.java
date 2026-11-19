package project.illit.smedia.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.user.dto.FollowActionResponse;
import project.illit.smedia.user.dto.FollowStatusResponse;
import project.illit.smedia.user.dto.FollowUserResponse;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.Follow;
import project.illit.smedia.user.entity.FollowId;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.FollowRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public FollowService(
            FollowRepository followRepository,
            UserRepository userRepository,
            ArtistProfileRepository artistProfileRepository
    ) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional
    public FollowActionResponse followUser(Integer followerId, Integer targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User target = requireActiveUser(targetUserId);

        FollowId followId = new FollowId(followerId, targetUserId);
        if (followRepository.existsById(followId)) {
            throw new ConflictException("You are already following this user");
        }

        Follow follow = Follow.builder()
                .id(followId)
                .follower(follower)
                .following(target)
                .build();
        followRepository.save(follow);

        userRepository.incrementTotalFollowing(followerId);
        userRepository.incrementTotalFollower(targetUserId);
        artistProfileRepository.incrementFollowersCount(targetUserId);

        int newTargetFollowers = (target.getTotalFollower() != null ? target.getTotalFollower() : 0) + 1;
        int newMyFollowing = (follower.getTotalFollowing() != null ? follower.getTotalFollowing() : 0) + 1;
        return new FollowActionResponse(targetUserId, true, newTargetFollowers, newMyFollowing);
    }

    @Transactional
    public FollowActionResponse unfollowUser(Integer followerId, Integer targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new BadRequestException("You cannot unfollow yourself");
        }

        FollowId followId = new FollowId(followerId, targetUserId);
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not following this user"));

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        followRepository.delete(follow);

        userRepository.decrementTotalFollowing(followerId);
        userRepository.decrementTotalFollower(targetUserId);
        artistProfileRepository.decrementFollowersCount(targetUserId);

        int newTargetFollowers = Math.max(0, (target.getTotalFollower() != null ? target.getTotalFollower() : 0) - 1);
        int newMyFollowing = Math.max(0, (follower.getTotalFollowing() != null ? follower.getTotalFollowing() : 0) - 1);
        return new FollowActionResponse(targetUserId, false, newTargetFollowers, newMyFollowing);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowers(Integer targetUserId, Integer viewerId, Pageable pageable) {
        requireActiveUser(targetUserId);
        Page<Follow> page = followRepository.findById_FollowingIdOrderByCreatedAtDesc(targetUserId, pageable);

        List<User> users = page.getContent().stream().map(Follow::getFollower).toList();
        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfiles(users);
        Set<Integer> followedByViewer = loadFollowedIds(viewerId, users);

        Page<FollowUserResponse> mapped = page.map(f -> {
            User u = f.getFollower();
            return FollowUserResponse.from(
                    u,
                    artistProfiles.get(u.getId()),
                    followedByViewer.contains(u.getId()),
                    f.getCreatedAt()
            );
        });
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<FollowUserResponse> getFollowing(Integer userId, Integer viewerId, Pageable pageable) {
        requireActiveUser(userId);
        Page<Follow> page = followRepository.findById_FollowerIdOrderByCreatedAtDesc(userId, pageable);

        List<User> users = page.getContent().stream().map(Follow::getFollowing).toList();
        Map<Integer, ArtistProfile> artistProfiles = loadArtistProfiles(users);
        Set<Integer> followedByViewer = loadFollowedIds(viewerId, users);

        Page<FollowUserResponse> mapped = page.map(f -> {
            User u = f.getFollowing();
            return FollowUserResponse.from(
                    u,
                    artistProfiles.get(u.getId()),
                    followedByViewer.contains(u.getId()),
                    f.getCreatedAt()
            );
        });
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse getFollowStatus(Integer viewerId, Integer targetUserId) {
        requireActiveUser(targetUserId);
        if (viewerId.equals(targetUserId)) {
            return new FollowStatusResponse(targetUserId, false, false, false);
        }
        boolean following = followRepository.existsById(new FollowId(viewerId, targetUserId));
        boolean followedBy = followRepository.existsById(new FollowId(targetUserId, viewerId));
        return new FollowStatusResponse(targetUserId, following, followedBy, following && followedBy);
    }

    private User requireActiveUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return user;
    }

    private Map<Integer, ArtistProfile> loadArtistProfiles(List<User> users) {
        Set<Integer> artistIds = new HashSet<>();
        for (User u : users) {
            if (u != null && u.getRole() != null && RoleName.ARTIST.name().equalsIgnoreCase(u.getRole().getName())) {
                artistIds.add(u.getId());
            }
        }
        if (artistIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, ArtistProfile> map = new HashMap<>();
        for (ArtistProfile profile : artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(artistIds)) {
            map.put(profile.getUserId(), profile);
        }
        return map;
    }

    private Set<Integer> loadFollowedIds(Integer viewerId, List<User> users) {
        if (viewerId == null || users.isEmpty()) {
            return Collections.emptySet();
        }
        List<Integer> userIds = users.stream().map(User::getId).toList();
        return new HashSet<>(followRepository.findFollowedUserIdsIn(viewerId, userIds));
    }
}
