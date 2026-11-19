package project.illit.smedia.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateArtistProfileRequest(
        @NotBlank(message = "Stage name is required")
        @Size(min = 1, max = 255, message = "Stage name must not exceed 255 characters")
        String stageName,

        String bio,

        @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
        String avatar
) {
}
