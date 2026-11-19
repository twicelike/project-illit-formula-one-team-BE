package project.illit.smedia.post.dto;

import project.illit.smedia.user.entity.ArtistProfile;

public record ArtistBadgeActor(
        Integer artistUserId,
        String stageName,
        String avatar
) {
    public static ArtistBadgeActor from(ArtistProfile profile) {
        String avatar = profile.getAvatar();
        if ((avatar == null || avatar.isBlank()) && profile.getUser() != null) {
            avatar = profile.getUser().getAvatar();
        }
        return new ArtistBadgeActor(profile.getUserId(), profile.getStageName(), avatar);
    }
}
