package project.illit.smedia.post.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdatePostRequest(
        @Size(max = 5000, message = "Post content must not exceed 5000 characters")
        String content,

        @Size(max = 10, message = "A post can attach at most 10 media items")
        List<@Valid MediaItemRequest> media
) {
}
