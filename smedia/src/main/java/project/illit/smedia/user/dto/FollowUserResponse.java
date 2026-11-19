package project.illit.smedia.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FollowUserResponse(
        Integer userId,
        String name,
        String stageName,
        String avatar,
        String role,
        boolean verified,
        Integer totalFollower,
        boolean followedByMe,
        LocalDateTime followedAt
) {
    public static FollowUserResponse from(
            User user,
            ArtistProfile artistProfile,
            boolean followedByMe,
            LocalDateTime followedAt
    ) {
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        boolean verified = RoleName.ARTIST.name().equals(roleName);
        String avatar = (artistProfile != null && artistProfile.getAvatar() != null && !artistProfile.getAvatar().isBlank())
                ? artistProfile.getAvatar()
                : user.getAvatar();

        return new FollowUserResponse(
                user.getId(),
                user.getName(),
                artistProfile != null ? artistProfile.getStageName() : null,
                avatar,
                roleName,
                verified,
                user.getTotalFollower() != null ? user.getTotalFollower() : 0,
                followedByMe,
                followedAt
        );
    }
}
