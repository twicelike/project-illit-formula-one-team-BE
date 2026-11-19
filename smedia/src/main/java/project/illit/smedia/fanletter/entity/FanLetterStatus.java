package project.illit.smedia.fanletter.entity;

import project.illit.smedia.common.exception.BadRequestException;

public enum FanLetterStatus {
    PUBLISHED,
    HIDDEN,
    DELETED;

    public static FanLetterStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Fan letter status is required");
        }
        try {
            return FanLetterStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid fan letter status: " + value + ". Allowed values: PUBLISHED, HIDDEN, DELETED");
        }
    }
}
