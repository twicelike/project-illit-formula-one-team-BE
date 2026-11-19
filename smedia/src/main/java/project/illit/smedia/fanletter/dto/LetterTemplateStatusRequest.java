package project.illit.smedia.fanletter.dto;

import jakarta.validation.constraints.NotNull;

public record LetterTemplateStatusRequest(
        @NotNull(message = "isActive is required")
        Boolean isActive
) {
}
