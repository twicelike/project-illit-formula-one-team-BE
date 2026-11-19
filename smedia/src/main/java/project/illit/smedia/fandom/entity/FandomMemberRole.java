package project.illit.smedia.fandom.entity;

import project.illit.smedia.common.exception.BadRequestException;

public enum FandomMemberRole {
    MEMBER,
    VIP_MEMBER,
    ARTIST,
    MODERATOR;

    public static FandomMemberRole fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Fandom member role is required");
        }
        try {
            return FandomMemberRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid fandom member role: " + value + ". Allowed values: MEMBER, VIP_MEMBER, ARTIST, MODERATOR");
        }
    }
}
