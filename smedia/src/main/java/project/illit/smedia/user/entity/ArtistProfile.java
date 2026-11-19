package project.illit.smedia.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.illit.smedia.fandom.entity.Company;

import java.time.LocalDateTime;

@Entity
@Table(name = "artist_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistProfile {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "stage_name", nullable = false, length = 255)
    private String stageName;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 255)
    private String avatar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Builder.Default
    @Column(name = "followers_count")
    private Integer followersCount = 0;

    @Column(name = "debut_date")
    private LocalDateTime debutDate;

    @PrePersist
    protected void onCreate() {
        if (this.followersCount == null) {
            this.followersCount = 0;
        }
    }
}
