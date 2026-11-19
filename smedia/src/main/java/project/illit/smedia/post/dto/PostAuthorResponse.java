package project.illit.smedia.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PostAuthorResponse(
        Integer id,
        String name,
        String stageName,
        String avatar,
        String role,
        boolean verified
) {
    public static PostAuthorResponse from(User user, ArtistProfile artistProfile) {
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        boolean isArtist = RoleName.ARTIST.name().equals(roleName);
        String resolvedStageName = artistProfile != null ? artistProfile.getStageName() : null;
        String resolvedAvatar = (artistProfile != null && artistProfile.getAvatar() != null && !artistProfile.getAvatar().isBlank())
                ? artistProfile.getAvatar()
                : user.getAvatar();

        return new PostAuthorResponse(
                user.getId(),
                user.getName(),
                resolvedStageName,
                resolvedAvatar,
                roleName,
                isArtist
        );
    }
}
