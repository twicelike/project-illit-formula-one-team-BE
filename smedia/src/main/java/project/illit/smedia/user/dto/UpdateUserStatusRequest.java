package project.illit.smedia.user.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull(message = "isActive flag is required")
        Boolean isActive
) {
}
