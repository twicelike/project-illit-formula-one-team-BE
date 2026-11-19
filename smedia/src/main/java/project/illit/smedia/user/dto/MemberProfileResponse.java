package project.illit.smedia.user.dto;

import project.illit.smedia.user.entity.MemberProfile;

public record MemberProfileResponse(
        String membershipTier,
        Integer totalLoyaltyPoints
) {
    public static MemberProfileResponse from(MemberProfile profile) {
        if (profile == null) {
            return null;
        }
        return new MemberProfileResponse(
                profile.getMembershipTier(),
                profile.getTotalLoyaltyPoints()
        );
    }
}
