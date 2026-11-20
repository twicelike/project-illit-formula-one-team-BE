package project.illit.smedia.fanletter.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.illit.smedia.fanletter.entity.StickerItem;

import java.util.List;

public record UpdateFanLetterRequest(
        Integer templateId,

        @NotBlank(message = "Fan letter content is required")
        @Size(max = 3000, message = "Fan letter content must not exceed 3000 characters")
        String content,

        Integer backgroundImageId,

        @Size(max = 255, message = "Background image URL must not exceed 255 characters")
        @Pattern(regexp = "^$|^(/|https?://).+", message = "Background image URL must start with http://, https://, or /")
        String backgroundImageUrl,

        Boolean clearBackgroundImage,

        @Size(max = 30, message = "A fan letter can contain at most 30 stickers")
        List<@Valid StickerItem> stickerList,

        Boolean isPublic
) {
}
