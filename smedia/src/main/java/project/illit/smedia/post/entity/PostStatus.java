package project.illit.smedia.post.entity;

import project.illit.smedia.common.exception.BadRequestException;

public enum PostStatus {
    PUBLISHED,
    HIDDEN,
    DELETED;

    public static PostStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Post status is required");
        }
        try {
            return PostStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid post status: " + value + ". Allowed values: PUBLISHED, HIDDEN, DELETED");
        }
    }
}
