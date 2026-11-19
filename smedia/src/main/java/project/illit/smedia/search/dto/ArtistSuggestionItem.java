package project.illit.smedia.search.dto;

import project.illit.smedia.user.entity.ArtistProfile;

public record ArtistSuggestionItem(
        Integer userId,
        String stageName,
        String avatar,
        String companyName,
        boolean verified
) {
    public static ArtistSuggestionItem from(ArtistProfile profile) {
        String resolvedAvatar = profile.getAvatar();
        if ((resolvedAvatar == null || resolvedAvatar.isBlank()) && profile.getUser() != null) {
            resolvedAvatar = profile.getUser().getAvatar();
        }
        return new ArtistSuggestionItem(
                profile.getUserId(),
                profile.getStageName(),
                resolvedAvatar,
                profile.getCompany() != null ? profile.getCompany().getName() : null,
                true
        );
    }
}
