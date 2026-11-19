package project.illit.smedia.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        Integer id,
        String email,
        String name,
        String avatar,
        String role,
        boolean verified,
        Integer totalFollower,
        Integer totalFollowing,
        Boolean isActive,
        LocalDateTime createdAt,
        MemberProfileResponse memberProfile,
        ArtistProfileResponse artistProfile
) {
    public static UserProfileResponse from(
            User user,
            MemberProfile memberProfile,
            ArtistProfile artistProfile,
            boolean includePrivateFields
    ) {
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        return new UserProfileResponse(
                user.getId(),
                includePrivateFields ? user.getEmail() : null,
                user.getName(),
                user.getAvatar(),
                roleName,
                RoleName.ARTIST.name().equals(roleName),
                user.getTotalFollower(),
                user.getTotalFollowing(),
                includePrivateFields ? user.getIsActive() : null,
                user.getCreatedAt(),
                MemberProfileResponse.from(memberProfile),
                ArtistProfileResponse.from(artistProfile)
        );
    }
}
