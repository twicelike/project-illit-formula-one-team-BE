package project.illit.smedia.fanletter.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LetterTemplateRequest(
        @NotBlank(message = "Template image URL is required")
        @Size(max = 255, message = "Template image URL must not exceed 255 characters")
        @Pattern(regexp = "^https?://.+", message = "Template image URL must start with http:// or https://")
        String imageUrl,

        Boolean isPremium,

        @Min(value = 0, message = "Price must be >= 0")
        Integer price,

        Boolean isActive
) {
}
