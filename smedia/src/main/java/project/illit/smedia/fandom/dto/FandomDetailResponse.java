package project.illit.smedia.fandom.dto;

import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.user.dto.ArtistProfileResponse;

import java.time.LocalDateTime;
import java.util.List;

public record FandomDetailResponse(
        Integer id,
        String name,
        String status,
        String bannerUrl,
        LocalDateTime createdAt,
        CompanyResponse company,
        long memberCount,
        long artistCount,
        boolean joined,
        String myRole,
        List<ArtistProfileResponse> artists,
        List<String> availableTabs
) {
    public static final List<String> DEFAULT_TABS = List.of(
            "FEED",
            "ARTIST",
            "FAN_LETTER",
            "LIVE",
            "MEDIA",
            "EVENTS"
    );

    public static FandomDetailResponse from(
            OfficialFandom fandom,
            long memberCount,
            List<ArtistProfileResponse> artists,
            String myRole
    ) {
        return new FandomDetailResponse(
                fandom.getId(),
                fandom.getName(),
                fandom.getStatus(),
                fandom.getBannerUrl(),
                fandom.getCreatedAt(),
                CompanyResponse.from(fandom.getCompany()),
                memberCount,
                artists.size(),
                myRole != null,
                myRole,
                artists,
                DEFAULT_TABS
        );
    }
}
