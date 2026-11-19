package project.illit.smedia.fanletter.entity;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fan_letter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FanLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", referencedColumnName = "user_id")
    private ArtistProfile artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private LetterTemplate template;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "background_image_id")
    private Integer backgroundImageId;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sticker_list", columnDefinition = "jsonb")
    private List<StickerItem> stickerList = new ArrayList<>();

    @Builder.Default
    @Column(name = "is_public")
    private Boolean isPublic = true;

    @Builder.Default
    @Column(length = 50)
    private String status = FanLetterStatus.PUBLISHED.name();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.stickerList == null) {
            this.stickerList = new ArrayList<>();
        }
        if (this.isPublic == null) {
            this.isPublic = true;
        }
        if (this.status == null || this.status.isBlank()) {
            this.status = FanLetterStatus.PUBLISHED.name();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
