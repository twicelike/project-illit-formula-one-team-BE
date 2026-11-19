package project.illit.smedia.post.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ArtistInteractionSummary(
        boolean artistReplied,
        boolean artistLiked,
        List<ArtistBadgeActor> repliedArtists,
        List<ArtistBadgeActor> likedArtists,
        String highlightText
) {
    public static ArtistInteractionSummary of(
            List<ArtistBadgeActor> repliedArtists,
            List<ArtistBadgeActor> likedArtists
    ) {
        List<ArtistBadgeActor> replies = repliedArtists != null ? repliedArtists : List.of();
        List<ArtistBadgeActor> likes = likedArtists != null ? likedArtists : List.of();

        boolean hasReplies = !replies.isEmpty();
        boolean hasLikes = !likes.isEmpty();
        String highlight = buildHighlightText(replies, likes);

        return new ArtistInteractionSummary(hasReplies, hasLikes, replies, likes, highlight);
    }

    private static String buildHighlightText(List<ArtistBadgeActor> replies, List<ArtistBadgeActor> likes) {
        if (!replies.isEmpty()) {
            String first = replies.get(0).stageName();
            if (replies.size() == 1) {
                return first + " đã bình luận";
            }
            return first + " và " + (replies.size() - 1) + " nghệ sĩ khác đã bình luận";
        }
        if (!likes.isEmpty()) {
            String first = likes.get(0).stageName();
            if (likes.size() == 1) {
                return first + " đã thả tim";
            }
            return first + " và " + (likes.size() - 1) + " nghệ sĩ khác đã thả tim";
        }
        return null;
    }
}
