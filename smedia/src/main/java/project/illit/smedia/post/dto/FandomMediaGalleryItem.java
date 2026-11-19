package project.illit.smedia.post.dto;

import java.time.LocalDateTime;

public record FandomMediaGalleryItem(
        Integer mediaId,
        Integer postId,
        String mediaUrl,
        String thumbnail,
        String authorName,
        boolean artistPost,
        LocalDateTime createdAt
) {
}
