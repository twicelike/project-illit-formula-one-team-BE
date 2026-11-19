package project.illit.smedia.search.dto;

import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.user.dto.ArtistProfileResponse;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        List<FandomSummaryResponse> fandoms,
        List<ArtistProfileResponse> artists,
        List<PostResponse> posts,
        SearchTotals totals
) {
}
