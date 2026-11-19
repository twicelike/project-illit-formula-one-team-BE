package project.illit.smedia.fanletter.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFanLetterStatusRequest(
        @NotBlank(message = "Status is required")
        String status
) {
}
