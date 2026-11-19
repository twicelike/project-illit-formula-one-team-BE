package project.illit.smedia.post.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostStatusRequest(
        @NotBlank(message = "Post status is required")
        String status
) {
}
