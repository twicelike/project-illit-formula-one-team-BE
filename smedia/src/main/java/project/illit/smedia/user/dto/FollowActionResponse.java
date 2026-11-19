package project.illit.smedia.user.dto;

public record FollowActionResponse(
        Integer targetUserId,
        boolean following,
        int targetTotalFollower,
        int myTotalFollowing
) {
}
