package project.illit.smedia.interaction.dto;

public record ReactionToggleResponse(
        Integer targetId,
        String targetType,
        boolean reacted,
        int totalReactions
) {
}
