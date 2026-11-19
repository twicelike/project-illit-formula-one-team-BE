package project.illit.smedia.fanletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "letter_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LetterTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Builder.Default
    @Column(name = "is_premium")
    private Boolean isPremium = false;

    @Builder.Default
    @Column(name = "price")
    private Integer price = 0;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (this.isPremium == null) {
            this.isPremium = false;
        }
        if (this.price == null) {
            this.price = 0;
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
    }
}
