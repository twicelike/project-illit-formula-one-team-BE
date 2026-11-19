package project.illit.smedia.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.user.dto.ChangePasswordRequest;
import project.illit.smedia.user.dto.UpdateProfileRequest;
import project.illit.smedia.user.dto.UserProfileResponse;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserRepository userRepository,
            MemberProfileRepository memberProfileRepository,
            ArtistProfileRepository artistProfileRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(Integer userId) {
        User user = requireUser(userId);
        return buildProfileResponse(user, true);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(Integer userId) {
        User user = requireUser(userId);
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return buildProfileResponse(user, false);
    }

    @Transactional
    public UserProfileResponse updateMyProfile(Integer userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setName(request.name().trim());
        if (request.avatar() != null) {
            user.setAvatar(request.avatar().trim());
        }
        User updated = userRepository.save(user);
        return buildProfileResponse(updated, true);
    }

    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserProfileResponse> listUsersForAdmin(String role, Pageable pageable) {
        Page<User> page = (role != null && !role.isBlank())
                ? userRepository.findByRole_NameIgnoreCase(role.trim(), pageable)
                : userRepository.findAll(pageable);

        Page<UserProfileResponse> mapped = page.map(user -> buildProfileResponse(user, true));
        return PageResponse.from(mapped);
    }

    @Transactional
    public UserProfileResponse updateUserStatus(Integer adminUserId, Integer targetUserId, boolean active) {
        if (adminUserId.equals(targetUserId) && !active) {
            throw new BadRequestException("Admin cannot disable their own account");
        }
        User target = requireUser(targetUserId);
        target.setIsActive(active);
        User updated = userRepository.save(target);
        return buildProfileResponse(updated, true);
    }

    private User requireUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private UserProfileResponse buildProfileResponse(User user, boolean includePrivateFields) {
        MemberProfile memberProfile = memberProfileRepository.findById(user.getId()).orElse(null);
        ArtistProfile artistProfile = artistProfileRepository.findWithDetailsByUserId(user.getId()).orElse(null);
        return UserProfileResponse.from(user, memberProfile, artistProfile, includePrivateFields);
    }
}
