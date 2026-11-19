package project.illit.smedia.fandom.dto;

import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;

public record FandomMemberResponse(
        Long membershipId,
        Integer fandomId,
        Integer userId,
        String name,
        String avatar,
        String userRole,
        String fandomRole,
        boolean verified,
        LocalDateTime joinedAt
) {
    public static FandomMemberResponse from(FandomMember member) {
        User user = member.getUser();
        String systemRole = user != null && user.getRole() != null
                ? user.getRole().getName().toUpperCase()
                : RoleName.FAN.name();
        boolean isVerified = RoleName.ARTIST.name().equals(systemRole)
                || "ARTIST".equalsIgnoreCase(member.getRole());

        return new FandomMemberResponse(
                member.getId(),
                member.getFandom() != null ? member.getFandom().getId() : null,
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getAvatar() : null,
                systemRole,
                member.getRole(),
                isVerified,
                member.getJoinedAt()
        );
    }
}
