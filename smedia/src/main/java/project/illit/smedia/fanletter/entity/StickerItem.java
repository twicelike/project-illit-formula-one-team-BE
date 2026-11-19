package project.illit.smedia.fanletter.entity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StickerItem {

    @NotBlank(message = "Sticker id is required")
    @Size(max = 100, message = "Sticker id must not exceed 100 characters")
    private String id;

    @NotNull(message = "Sticker x coordinate is required")
    @DecimalMin(value = "0.0", message = "Sticker x must be >= 0")
    @DecimalMax(value = "2000.0", message = "Sticker x must be <= 2000")
    private Double x;

    @NotNull(message = "Sticker y coordinate is required")
    @DecimalMin(value = "0.0", message = "Sticker y must be >= 0")
    @DecimalMax(value = "2000.0", message = "Sticker y must be <= 2000")
    private Double y;

    @NotNull(message = "Sticker scale is required")
    @DecimalMin(value = "0.1", message = "Sticker scale must be >= 0.1")
    @DecimalMax(value = "5.0", message = "Sticker scale must be <= 5.0")
    private Double scale;

    @DecimalMin(value = "-360.0", message = "Sticker rotation must be >= -360")
    @DecimalMax(value = "360.0", message = "Sticker rotation must be <= 360")
    private Double rotation;
}
