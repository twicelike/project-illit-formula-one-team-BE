package project.illit.smedia.fandom.dto;

import project.illit.smedia.fandom.entity.OfficialFandom;

import java.time.LocalDateTime;

public record FandomSummaryResponse(
        Integer id,
        String name,
        String status,
        String bannerUrl,
        LocalDateTime createdAt,
        CompanyResponse company,
        long memberCount,
        long artistCount,
        boolean joined,
        String myRole
) {
    public static FandomSummaryResponse from(
            OfficialFandom fandom,
            long memberCount,
            long artistCount,
            String myRole
    ) {
        return new FandomSummaryResponse(
                fandom.getId(),
                fandom.getName(),
                fandom.getStatus(),
                fandom.getBannerUrl(),
                fandom.getCreatedAt(),
                CompanyResponse.from(fandom.getCompany()),
                memberCount,
                artistCount,
                myRole != null,
                myRole
        );
    }
}
