package project.illit.smedia.post.dto;

import project.illit.smedia.post.entity.Media;

import java.time.LocalDateTime;

public record MediaResponse(
        Integer id,
        String mediaUrl,
        String thumbnail,
        LocalDateTime createdAt
) {
    public static MediaResponse from(Media media) {
        return new MediaResponse(
                media.getId(),
                media.getMediaUrl(),
                media.getThumbnail(),
                media.getCreatedAt()
        );
    }
}
