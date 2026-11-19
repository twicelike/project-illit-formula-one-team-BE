package project.illit.smedia.fandom.dto;

import jakarta.validation.constraints.NotNull;

public record AssignArtistRequest(
        @NotNull(message = "Artist user id is required")
        Integer artistUserId
) {
}
