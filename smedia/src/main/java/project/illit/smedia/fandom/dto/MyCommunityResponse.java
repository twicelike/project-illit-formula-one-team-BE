package project.illit.smedia.fandom.dto;

import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.OfficialFandom;

import java.time.LocalDateTime;

public record MyCommunityResponse(
        Integer fandomId,
        String fandomName,
        String bannerUrl,
        String status,
        CompanyResponse company,
        long memberCount,
        long artistCount,
        String myRole,
        LocalDateTime joinedAt
) {
    public static MyCommunityResponse from(
            FandomMember membership,
            long memberCount,
            long artistCount
    ) {
        OfficialFandom fandom = membership.getFandom();
        return new MyCommunityResponse(
                fandom.getId(),
                fandom.getName(),
                fandom.getBannerUrl(),
                fandom.getStatus(),
                CompanyResponse.from(fandom.getCompany()),
                memberCount,
                artistCount,
                membership.getRole(),
                membership.getJoinedAt()
        );
    }
}
