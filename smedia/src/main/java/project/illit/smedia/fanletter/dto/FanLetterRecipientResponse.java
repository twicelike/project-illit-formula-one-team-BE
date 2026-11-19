package project.illit.smedia.fanletter.dto;

import project.illit.smedia.user.entity.ArtistProfile;

public record FanLetterRecipientResponse(
        Integer artistUserId,
        String stageName,
        String avatar,
        String companyName,
        boolean verified
) {
    public static FanLetterRecipientResponse from(ArtistProfile artist) {
        if (artist == null) {
            return null;
        }
        String avatar = artist.getAvatar();
        if ((avatar == null || avatar.isBlank()) && artist.getUser() != null) {
            avatar = artist.getUser().getAvatar();
        }
        return new FanLetterRecipientResponse(
                artist.getUserId(),
                artist.getStageName(),
                avatar,
                artist.getCompany() != null ? artist.getCompany().getName() : null,
                true
        );
    }
}
