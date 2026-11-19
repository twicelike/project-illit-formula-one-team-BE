package project.illit.smedia.interaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.post.dto.PostAuthorResponse;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommentResponse(
        Integer id,
        Integer postId,
        Integer parentId,
        PostAuthorResponse author,
        String content,
        Integer totalReactions,
        Integer totalReplies,
        boolean reactedByMe,
        boolean artistComment,
        boolean artistReplied,
        List<CommentResponse> previewReplies,
        LocalDateTime createdAt
) {
    public static CommentResponse from(
            Comment comment,
            PostAuthorResponse author,
            boolean reactedByMe,
            boolean artistReplied,
            List<CommentResponse> previewReplies
    ) {
        return new CommentResponse(
                comment.getId(),
                comment.getPost() != null ? comment.getPost().getId() : null,
                comment.getParent() != null ? comment.getParent().getId() : null,
                author,
                comment.getContent(),
                comment.getTotalReactions() != null ? comment.getTotalReactions() : 0,
                comment.getTotalReplies() != null ? comment.getTotalReplies() : 0,
                reactedByMe,
                author != null && author.verified(),
                artistReplied,
                previewReplies,
                comment.getCreatedAt()
        );
    }
}
