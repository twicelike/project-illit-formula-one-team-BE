package project.illit.smedia.post.dto;

import project.illit.smedia.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

public record PostResponse(
        Integer id,
        Integer fandomId,
        String fandomName,
        PostAuthorResponse author,
        String content,
        List<MediaResponse> media,
        Integer totalReactions,
        Integer totalComments,
        boolean artistPost,
        boolean reactedByMe,
        ArtistInteractionSummary artistInteractions,
        String status,
        LocalDateTime createdAt
) {
    public static PostResponse from(
            Post post,
            PostAuthorResponse author,
            List<MediaResponse> media,
            boolean reactedByMe,
            ArtistInteractionSummary artistInteractions
    ) {
        return new PostResponse(
                post.getId(),
                post.getFandom() != null ? post.getFandom().getId() : null,
                post.getFandom() != null ? post.getFandom().getName() : null,
                author,
                post.getContent(),
                media != null ? media : List.of(),
                post.getTotalReactions() != null ? post.getTotalReactions() : 0,
                post.getTotalComments() != null ? post.getTotalComments() : 0,
                Boolean.TRUE.equals(post.getIsArtistPost()),
                reactedByMe,
                artistInteractions,
                post.getStatus(),
                post.getCreatedAt()
        );
    }
}
