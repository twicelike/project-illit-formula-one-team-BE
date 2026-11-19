package project.illit.smedia.fandom.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFandomMemberRoleRequest(
        @NotBlank(message = "Fandom member role is required")
        String role
) {
}
