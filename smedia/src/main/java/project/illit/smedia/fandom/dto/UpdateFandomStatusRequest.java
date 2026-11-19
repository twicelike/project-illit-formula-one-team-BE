package project.illit.smedia.fandom.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFandomStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
