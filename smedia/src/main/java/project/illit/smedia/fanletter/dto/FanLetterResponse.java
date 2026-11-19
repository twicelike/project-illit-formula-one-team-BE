package project.illit.smedia.fanletter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.fanletter.entity.FanLetter;
import project.illit.smedia.fanletter.entity.StickerItem;
import project.illit.smedia.post.dto.MediaResponse;
import project.illit.smedia.post.entity.Media;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FanLetterResponse(
        Integer id,
        FanLetterSenderResponse sender,
        FanLetterRecipientResponse recipient,
        LetterTemplateResponse template,
        String content,
        Integer backgroundImageId,
        MediaResponse backgroundImage,
        List<StickerItem> stickerList,
        boolean isPublic,
        String status,
        LocalDateTime createdAt
) {
    public static FanLetterResponse from(FanLetter letter, Media backgroundMedia) {
        return new FanLetterResponse(
                letter.getId(),
                FanLetterSenderResponse.from(letter.getUser()),
                FanLetterRecipientResponse.from(letter.getArtist()),
                LetterTemplateResponse.from(letter.getTemplate()),
                letter.getContent(),
                letter.getBackgroundImageId(),
                backgroundMedia != null ? MediaResponse.from(backgroundMedia) : null,
                letter.getStickerList() != null ? letter.getStickerList() : List.of(),
                Boolean.TRUE.equals(letter.getIsPublic()),
                letter.getStatus(),
                letter.getCreatedAt()
        );
    }
}
