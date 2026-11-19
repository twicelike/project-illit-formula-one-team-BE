package project.illit.smedia.post.dto;

public record MediaUploadResponse(
        String mediaUrl,
        String fileName,
        String contentType,
        long sizeBytes,
        String bucket
) {
}
