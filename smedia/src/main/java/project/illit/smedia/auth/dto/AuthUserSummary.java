package project.illit.smedia.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserSummary {

    private Integer id;
    private String email;
    private String name;
    private String avatar;
    private String role;
    private boolean verified;

    public static AuthUserSummary from(User user) {
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        return AuthUserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .role(roleName)
                .verified(RoleName.ARTIST.name().equals(roleName))
                .build();
    }
}
