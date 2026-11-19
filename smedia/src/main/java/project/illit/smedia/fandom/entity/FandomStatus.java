package project.illit.smedia.fandom.entity;

import project.illit.smedia.common.exception.BadRequestException;

public enum FandomStatus {
    ACTIVE,
    INACTIVE,
    ARCHIVED;

    public static FandomStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return FandomStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid fandom status: " + value + ". Allowed values: ACTIVE, INACTIVE, ARCHIVED");
        }
    }
}
