package project.illit.smedia.fanletter.dto;

import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;

public record FanLetterSenderResponse(
        Integer id,
        String name,
        String avatar,
        String role
) {
    public static FanLetterSenderResponse from(User user) {
        if (user == null) {
            return null;
        }
        String roleName = user.getRole() != null ? user.getRole().getName().toUpperCase() : RoleName.FAN.name();
        return new FanLetterSenderResponse(user.getId(), user.getName(), user.getAvatar(), roleName);
    }
}
