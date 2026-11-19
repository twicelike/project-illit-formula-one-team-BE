package project.illit.smedia.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "total_reactions")
    private Integer totalReactions = 0;

    @Builder.Default
    @Column(name = "total_comments")
    private Integer totalComments = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fandom_id")
    private OfficialFandom fandom;

    @Builder.Default
    @Column(name = "is_artist_post")
    private Boolean isArtistPost = false;

    @Builder.Default
    @Column(length = 50)
    private String status = PostStatus.PUBLISHED.name();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.totalReactions == null) {
            this.totalReactions = 0;
        }
        if (this.totalComments == null) {
            this.totalComments = 0;
        }
        if (this.isArtistPost == null) {
            this.isArtistPost = false;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = PostStatus.PUBLISHED.name();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
