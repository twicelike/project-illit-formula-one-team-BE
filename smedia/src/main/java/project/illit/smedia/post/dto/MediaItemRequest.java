package project.illit.smedia.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MediaItemRequest(
        @NotBlank(message = "Media URL is required")
        @Size(max = 255, message = "Media URL must not exceed 255 characters")
        @Pattern(regexp = "^(/|https?://).+", message = "Media URL must start with http://, https://, or /")
        String mediaUrl,

        @Size(max = 255, message = "Thumbnail URL must not exceed 255 characters")
        @Pattern(regexp = "^$|^(/|https?://).+", message = "Thumbnail URL must start with http://, https://, or /")
        String thumbnail
) {
}
