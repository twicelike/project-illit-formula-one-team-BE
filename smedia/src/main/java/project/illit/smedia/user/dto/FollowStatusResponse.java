package project.illit.smedia.user.dto;

public record FollowStatusResponse(
        Integer targetUserId,
        boolean following,
        boolean followedBy,
        boolean mutual
) {
}
