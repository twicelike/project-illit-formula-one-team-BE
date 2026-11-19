package project.illit.smedia.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.OfficialFandomRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.post.service.PostService;
import project.illit.smedia.search.dto.GlobalSearchResponse;
import project.illit.smedia.search.dto.SearchSuggestionsResponse;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private OfficialFandomRepository fandomRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private FandomService fandomService;
    @Mock
    private PostService postService;

    private SearchService searchService;
    private Company belift;
    private OfficialFandom illitFandom;
    private ArtistProfile yunahProfile;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(
                fandomRepository,
                artistProfileRepository,
                postRepository,
                fandomService,
                postService
        );
        belift = Company.builder().id(1).name("BELIFT LAB").build();
        illitFandom = OfficialFandom.builder().id(1).name("ILLIT").company(belift).status("ACTIVE").build();
        User yunahUser = User.builder().id(2).name("Noh Yunah").avatar("https://cdn.example.com/yunah.jpg").build();
        yunahProfile = ArtistProfile.builder()
                .userId(2)
                .user(yunahUser)
                .stageName("YUNAH")
                .company(belift)
                .followersCount(3)
                .build();
    }

    @Test
    void normalizeKeywordAndEscapeLikeWildcards() {
        assertThat(searchService.normalizeKeyword("   ILLIT   album  ")).isEqualTo("ILLIT album");
        assertThat(searchService.escapeLikePattern("100%_a\\b")).isEqualTo("100\\%\\_a\\\\b");

        assertThatThrownBy(() -> searchService.normalizeKeyword("   "))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> searchService.normalizeKeyword("a".repeat(101)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void globalSearch_combinesFandomsArtistsPostsAndTotals() {
        PageRequest top5 = PageRequest.of(0, 5);
        Page<OfficialFandom> fandomPage = new PageImpl<>(List.of(illitFandom), top5, 1);
        Page<ArtistProfile> artistPage = new PageImpl<>(List.of(yunahProfile), top5, 1);
        Page<Post> postPage = new PageImpl<>(List.of(), top5, 0);

        FandomSummaryResponse fandomSummary = FandomSummaryResponse.from(illitFandom, 5L, 2L, "VIP_MEMBER");
        PageResponse<FandomSummaryResponse> fandomSummaryPage = PageResponse.<FandomSummaryResponse>builder()
                .items(List.of(fandomSummary))
                .totalElements(1)
                .build();
        PageResponse<PostResponse> emptyPostPage = PageResponse.<PostResponse>builder()
                .items(List.of())
                .totalElements(0)
                .build();

        when(fandomRepository.searchActiveFandoms("%illit%", "illit%", false, 0, top5)).thenReturn(fandomPage);
        when(fandomService.buildSummaryPage(fandomPage, 5)).thenReturn(fandomSummaryPage);
        when(artistProfileRepository.searchActiveArtists("%illit%", "illit%", false, 0, false, 0, top5))
                .thenReturn(artistPage);
        when(postRepository.searchPublishedPostsLatest("%illit%", false, 0, false, false, false, top5))
                .thenReturn(postPage);
        when(postService.enrichPostPage(postPage, 5)).thenReturn(emptyPostPage);

        GlobalSearchResponse res = searchService.globalSearch("  ILLIT ", null, 5, 5);

        assertThat(res.query()).isEqualTo("ILLIT");
        assertThat(res.fandoms()).hasSize(1);
        assertThat(res.artists()).hasSize(1);
        assertThat(res.artists().get(0).stageName()).isEqualTo("YUNAH");
        assertThat(res.totals().fandoms()).isEqualTo(1);
        assertThat(res.totals().artists()).isEqualTo(1);
        assertThat(res.totals().posts()).isZero();
    }

    @Test
    void searchPosts_escapesWildcardsValidatesFiltersAndRejectsInactiveFandom() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(fandomService.requireFandom(1)).thenReturn(illitFandom);
        when(postRepository.searchPublishedPostsHot(
                eq("%50\\%%"), eq(true), eq(1), eq(true), eq(true), eq(true), eq(pageable)
        )).thenReturn(emptyPage);
        when(postService.enrichPostPage(emptyPage, 5)).thenReturn(
                PageResponse.<PostResponse>builder().items(List.of()).totalElements(0).build()
        );

        PageResponse<PostResponse> res = searchService.searchPosts("50%", 1, "ARTIST", true, "hot", 5, pageable);
        assertThat(res.getItems()).isEmpty();

        // Inactive fandom must throw 404
        OfficialFandom inactive = OfficialFandom.builder().id(9).name("OLD").status("INACTIVE").build();
        when(fandomService.requireFandom(9)).thenReturn(inactive);
        assertThatThrownBy(() -> searchService.searchPosts("hi", 9, "ALL", false, "latest", 5, pageable))
                .isInstanceOf(ResourceNotFoundException.class);

        // Invalid sortBy must throw 400
        assertThatThrownBy(() -> searchService.searchPosts("hi", null, "ALL", false, "random", 5, pageable))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getSuggestions_returnsLightweightFandomAndArtistMatches() {
        PageRequest top5 = PageRequest.of(0, 5);
        when(fandomRepository.searchActiveFandoms("%yu%", "yu%", false, 0, top5))
                .thenReturn(new PageImpl<>(List.of(), top5, 0));
        when(artistProfileRepository.searchActiveArtists("%yu%", "yu%", false, 0, false, 0, top5))
                .thenReturn(new PageImpl<>(List.of(yunahProfile), top5, 1));

        SearchSuggestionsResponse res = searchService.getSuggestions("yu", 5);

        assertThat(res.query()).isEqualTo("yu");
        assertThat(res.fandoms()).isEmpty();
        assertThat(res.artists()).hasSize(1);
        assertThat(res.artists().get(0).stageName()).isEqualTo("YUNAH");
        assertThat(res.artists().get(0).companyName()).isEqualTo("BELIFT LAB");
    }
}
