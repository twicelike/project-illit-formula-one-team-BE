package project.illit.smedia.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        @Size(max = 255, message = "Avatar URL must not exceed 255 characters")
        String avatar
) {
}
