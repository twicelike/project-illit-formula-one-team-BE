package project.illit.smedia.search.dto;

import project.illit.smedia.fandom.entity.OfficialFandom;

public record FandomSuggestionItem(
        Integer id,
        String name,
        String bannerUrl,
        String companyName
) {
    public static FandomSuggestionItem from(OfficialFandom fandom) {
        return new FandomSuggestionItem(
                fandom.getId(),
                fandom.getName(),
                fandom.getBannerUrl(),
                fandom.getCompany() != null ? fandom.getCompany().getName() : null
        );
    }
}
