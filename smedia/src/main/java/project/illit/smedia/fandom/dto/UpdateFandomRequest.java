package project.illit.smedia.fandom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateFandomRequest(
        @NotNull(message = "Company id is required")
        Integer companyId,

        @NotBlank(message = "Fandom name is required")
        @Size(min = 2, max = 255, message = "Fandom name must be between 2 and 255 characters")
        String name,

        @Size(max = 255, message = "Banner URL must not exceed 255 characters")
        String bannerUrl,

        @NotBlank(message = "Status is required")
        String status
) {
}
