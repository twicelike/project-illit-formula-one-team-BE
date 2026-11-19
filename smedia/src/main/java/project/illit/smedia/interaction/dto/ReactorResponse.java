package project.illit.smedia.interaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReactorResponse(
        Integer userId,
        String name,
        String stageName,
        String avatar,
        String role,
        boolean verified
) {
    public static ReactorResponse from(User user, ArtistProfile artistProfile) {
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        boolean verified = RoleName.ARTIST.name().equals(roleName);
        String avatar = (artistProfile != null && artistProfile.getAvatar() != null && !artistProfile.getAvatar().isBlank())
                ? artistProfile.getAvatar()
                : user.getAvatar();
        return new ReactorResponse(
                user.getId(),
                user.getName(),
                artistProfile != null ? artistProfile.getStageName() : null,
                avatar,
                roleName,
                verified
        );
    }
}
