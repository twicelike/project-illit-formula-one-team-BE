package project.illit.smedia.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateArtistAccountRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Legal/account name is required")
        @Size(min = 2, max = 255)
        String name,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100)
        String password,

        @NotBlank(message = "Stage name is required")
        @Size(max = 255)
        String stageName,

        String bio,

        @Size(max = 255)
        String avatar,

        @NotNull(message = "Company id is required")
        Integer companyId,

        LocalDateTime debutDate
) {
}
