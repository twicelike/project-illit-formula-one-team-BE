package project.illit.smedia.search.dto;

import java.util.List;

public record SearchSuggestionsResponse(
        String query,
        List<FandomSuggestionItem> fandoms,
        List<ArtistSuggestionItem> artists
) {
}
