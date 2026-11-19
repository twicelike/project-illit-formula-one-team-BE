package project.illit.smedia.search.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.OfficialFandomRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.post.dto.PostResponse;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.post.service.PostService;
import project.illit.smedia.search.dto.ArtistSuggestionItem;
import project.illit.smedia.search.dto.FandomSuggestionItem;
import project.illit.smedia.search.dto.GlobalSearchResponse;
import project.illit.smedia.search.dto.SearchSuggestionsResponse;
import project.illit.smedia.search.dto.SearchTotals;
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.repository.ArtistProfileRepository;

import java.util.List;
import java.util.Locale;

@Service
public class SearchService {

    private static final int MAX_KEYWORD_LENGTH = 100;

    private final OfficialFandomRepository fandomRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PostRepository postRepository;
    private final FandomService fandomService;
    private final PostService postService;

    public SearchService(
            OfficialFandomRepository fandomRepository,
            ArtistProfileRepository artistProfileRepository,
            PostRepository postRepository,
            FandomService fandomService,
            PostService postService
    ) {
        this.fandomRepository = fandomRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.postRepository = postRepository;
        this.fandomService = fandomService;
        this.postService = postService;
    }

    @Transactional(readOnly = true)
    public GlobalSearchResponse globalSearch(
            String rawQuery,
            Integer fandomId,
            int limit,
            Integer currentUserId
    ) {
        String query = normalizeKeyword(rawQuery);
        int boundedLimit = Math.max(1, Math.min(limit, 15));
        Pageable sectionPage = PageRequest.of(0, boundedLimit);

        if (fandomId != null) {
            ensureActiveFandom(fandomId);
        }

        PageResponse<FandomSummaryResponse> fandomPage = searchFandoms(query, null, currentUserId, sectionPage);
        PageResponse<ArtistProfileResponse> artistPage = searchArtists(query, null, fandomId, sectionPage);
        PageResponse<PostResponse> postPage = searchPosts(query, fandomId, "ALL", false, "latest", currentUserId, sectionPage);

        SearchTotals totals = new SearchTotals(
                fandomPage.getTotalElements(),
                artistPage.getTotalElements(),
                postPage.getTotalElements()
        );

        return new GlobalSearchResponse(
                query,
                fandomPage.getItems(),
                artistPage.getItems(),
                postPage.getItems(),
                totals
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<FandomSummaryResponse> searchFandoms(
            String rawQuery,
            Integer companyId,
            Integer currentUserId,
            Pageable pageable
    ) {
        String query = normalizeKeyword(rawQuery);
        String escaped = escapeLikePattern(query.toLowerCase(Locale.ROOT));
        String containsPattern = "%" + escaped + "%";
        String prefixPattern = escaped + "%";

        boolean filterByCompany = companyId != null;
        int resolvedCompanyId = filterByCompany ? companyId : 0;

        Page<OfficialFandom> page = fandomRepository.searchActiveFandoms(
                containsPattern,
                prefixPattern,
                filterByCompany,
                resolvedCompanyId,
                pageable
        );
        return fandomService.buildSummaryPage(page, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtistProfileResponse> searchArtists(
            String rawQuery,
            Integer companyId,
            Integer fandomId,
            Pageable pageable
    ) {
        String query = normalizeKeyword(rawQuery);
        String escaped = escapeLikePattern(query.toLowerCase(Locale.ROOT));
        String containsPattern = "%" + escaped + "%";
        String prefixPattern = escaped + "%";

        if (fandomId != null) {
            ensureActiveFandom(fandomId);
        }

        boolean filterByCompany = companyId != null;
        int resolvedCompanyId = filterByCompany ? companyId : 0;
        boolean filterByFandom = fandomId != null;
        int resolvedFandomId = filterByFandom ? fandomId : 0;

        Page<ArtistProfile> page = artistProfileRepository.searchActiveArtists(
                containsPattern,
                prefixPattern,
                filterByCompany,
                resolvedCompanyId,
                filterByFandom,
                resolvedFandomId,
                pageable
        );
        return PageResponse.from(page.map(ArtistProfileResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> searchPosts(
            String rawQuery,
            Integer fandomId,
            String tab,
            boolean hasMedia,
            String sortBy,
            Integer currentUserId,
            Pageable pageable
    ) {
        String query = normalizeKeyword(rawQuery);
        String escaped = escapeLikePattern(query.toLowerCase(Locale.ROOT));
        String containsPattern = "%" + escaped + "%";

        if (fandomId != null) {
            ensureActiveFandom(fandomId);
        }

        PostTabFilter tabFilter = PostTabFilter.fromString(tab);
        boolean sortHot = isHotSort(sortBy);

        boolean filterByFandom = fandomId != null;
        int resolvedFandomId = filterByFandom ? fandomId : 0;

        Page<Post> page = sortHot
                ? postRepository.searchPublishedPostsHot(
                        containsPattern,
                        filterByFandom,
                        resolvedFandomId,
                        tabFilter.filterByArtist,
                        tabFilter.artistPostValue,
                        hasMedia,
                        pageable
                )
                : postRepository.searchPublishedPostsLatest(
                        containsPattern,
                        filterByFandom,
                        resolvedFandomId,
                        tabFilter.filterByArtist,
                        tabFilter.artistPostValue,
                        hasMedia,
                        pageable
                );

        return postService.enrichPostPage(page, currentUserId);
    }

    @Transactional(readOnly = true)
    public SearchSuggestionsResponse getSuggestions(String rawQuery, int limit) {
        String query = normalizeKeyword(rawQuery);
        int boundedLimit = Math.max(1, Math.min(limit, 10));
        Pageable topN = PageRequest.of(0, boundedLimit);

        String escaped = escapeLikePattern(query.toLowerCase(Locale.ROOT));
        String containsPattern = "%" + escaped + "%";
        String prefixPattern = escaped + "%";

        List<FandomSuggestionItem> fandomSuggestions = fandomRepository
                .searchActiveFandoms(containsPattern, prefixPattern, false, 0, topN)
                .getContent().stream()
                .map(FandomSuggestionItem::from)
                .toList();

        List<ArtistSuggestionItem> artistSuggestions = artistProfileRepository
                .searchActiveArtists(containsPattern, prefixPattern, false, 0, false, 0, topN)
                .getContent().stream()
                .map(ArtistSuggestionItem::from)
                .toList();

        return new SearchSuggestionsResponse(query, fandomSuggestions, artistSuggestions);
    }

    String normalizeKeyword(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Search keyword 'q' must not be empty");
        }
        String collapsed = raw.trim().replaceAll("\\s+", " ");
        if (collapsed.length() > MAX_KEYWORD_LENGTH) {
            throw new BadRequestException("Search keyword must not exceed " + MAX_KEYWORD_LENGTH + " characters");
        }
        return collapsed;
    }

    String escapeLikePattern(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private void ensureActiveFandom(Integer fandomId) {
        OfficialFandom fandom = fandomService.requireFandom(fandomId);
        if (!FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }
    }

    private boolean isHotSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "LATEST".equalsIgnoreCase(sortBy.trim())) {
            return false;
        }
        if ("HOT".equalsIgnoreCase(sortBy.trim()) || "POPULAR".equalsIgnoreCase(sortBy.trim())) {
            return true;
        }
        throw new BadRequestException("Invalid sortBy: " + sortBy + ". Allowed values: latest, hot, popular");
    }

    private enum PostTabFilter {
        ALL(false, false),
        ARTIST(true, true),
        FAN(true, false);

        final boolean filterByArtist;
        final boolean artistPostValue;

        PostTabFilter(boolean filterByArtist, boolean artistPostValue) {
            this.filterByArtist = filterByArtist;
            this.artistPostValue = artistPostValue;
        }

        static PostTabFilter fromString(String value) {
            if (value == null || value.isBlank() || "FEED".equalsIgnoreCase(value.trim())) {
                return ALL;
            }
            try {
                return PostTabFilter.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid tab: " + value + ". Allowed values: ALL, FEED, ARTIST, FAN");
            }
        }
    }
}
