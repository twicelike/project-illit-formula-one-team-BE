package project.illit.smedia.fanletter.dto;

import project.illit.smedia.fanletter.entity.LetterTemplate;

public record LetterTemplateResponse(
        Integer id,
        String imageUrl,
        boolean isPremium,
        int price,
        boolean isActive
) {
    public static LetterTemplateResponse from(LetterTemplate template) {
        if (template == null) {
            return null;
        }
        return new LetterTemplateResponse(
                template.getId(),
                template.getImageUrl(),
                Boolean.TRUE.equals(template.getIsPremium()),
                template.getPrice() != null ? template.getPrice() : 0,
                Boolean.TRUE.equals(template.getIsActive())
        );
    }
}
