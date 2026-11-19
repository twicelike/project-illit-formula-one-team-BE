package project.illit.smedia.fandom.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import project.illit.smedia.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "fandom_member",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_fandom_member_fandom_user",
                columnNames = {"fandom_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FandomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fandom_id")
    private OfficialFandom fandom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(length = 50)
    private String role = FandomMemberRole.MEMBER.name();

    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        if (this.role == null || this.role.isBlank()) {
            this.role = FandomMemberRole.MEMBER.name();
        }
        if (this.joinedAt == null) {
            this.joinedAt = LocalDateTime.now();
        }
    }
}
