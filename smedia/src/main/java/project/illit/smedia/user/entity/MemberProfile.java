package project.illit.smedia.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "member_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberProfile {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Builder.Default
    @Column(name = "membership_tier", length = 50)
    private String membershipTier = "STANDARD";

    @Builder.Default
    @Column(name = "total_loyalty_points")
    private Integer totalLoyaltyPoints = 0;

    @PrePersist
    protected void onCreate() {
        if (this.membershipTier == null) {
            this.membershipTier = "STANDARD";
        }
        if (this.totalLoyaltyPoints == null) {
            this.totalLoyaltyPoints = 0;
        }
    }
}
