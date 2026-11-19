package project.illit.smedia.user.dto;

import project.illit.smedia.fandom.dto.CompanyResponse;
import project.illit.smedia.user.entity.ArtistProfile;

import java.time.LocalDateTime;

public record ArtistProfileResponse(
        Integer userId,
        String stageName,
        String bio,
        String avatar,
        Integer followersCount,
        LocalDateTime debutDate,
        boolean verified,
        CompanyResponse company
) {
    public static ArtistProfileResponse from(ArtistProfile profile) {
        if (profile == null) {
            return null;
        }
        String resolvedAvatar = profile.getAvatar();
        if ((resolvedAvatar == null || resolvedAvatar.isBlank()) && profile.getUser() != null) {
            resolvedAvatar = profile.getUser().getAvatar();
        }
        return new ArtistProfileResponse(
                profile.getUserId(),
                profile.getStageName(),
                profile.getBio(),
                resolvedAvatar,
                profile.getFollowersCount(),
                profile.getDebutDate(),
                true,
                CompanyResponse.from(profile.getCompany())
        );
    }
}
