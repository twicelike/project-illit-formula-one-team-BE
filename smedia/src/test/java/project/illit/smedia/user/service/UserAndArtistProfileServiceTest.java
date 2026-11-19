package project.illit.smedia.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.service.CompanyService;
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.dto.ChangePasswordRequest;
import project.illit.smedia.user.dto.CreateArtistAccountRequest;
import project.illit.smedia.user.dto.UserProfileResponse;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.RoleRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAndArtistProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberProfileRepository memberProfileRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private CompanyService companyService;

    private PasswordEncoder passwordEncoder;
    private UserProfileService userProfileService;
    private ArtistProfileService artistProfileService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userProfileService = new UserProfileService(
                userRepository,
                memberProfileRepository,
                artistProfileRepository,
                passwordEncoder
        );
        artistProfileService = new ArtistProfileService(
                artistProfileRepository,
                userRepository,
                roleRepository,
                companyService,
                passwordEncoder
        );
    }

    @Test
    void publicProfile_hidesEmailWhileMyProfileIncludesIt() {
        User fan = User.builder()
                .id(5)
                .email("gllit.minh@gmail.com")
                .name("Minh Nguyen")
                .role(Role.builder().id(3).name("FAN").build())
                .totalFollower(1)
                .totalFollowing(3)
                .isActive(true)
                .build();
        MemberProfile memberProfile = MemberProfile.builder()
                .userId(5)
                .user(fan)
                .membershipTier("VIP")
                .totalLoyaltyPoints(1250)
                .build();

        when(userRepository.findById(5)).thenReturn(Optional.of(fan));
        when(memberProfileRepository.findById(5)).thenReturn(Optional.of(memberProfile));
        when(artistProfileRepository.findWithDetailsByUserId(5)).thenReturn(Optional.empty());

        UserProfileResponse publicView = userProfileService.getPublicProfile(5);
        assertThat(publicView.email()).isNull();
        assertThat(publicView.memberProfile().membershipTier()).isEqualTo("VIP");

        UserProfileResponse privateView = userProfileService.getMyProfile(5);
        assertThat(privateView.email()).isEqualTo("gllit.minh@gmail.com");
        assertThat(privateView.memberProfile().totalLoyaltyPoints()).isEqualTo(1250);
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        User fan = User.builder()
                .id(5)
                .email("fan@example.com")
                .password(passwordEncoder.encode("correctPass"))
                .build();
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));

        assertThatThrownBy(() -> userProfileService.changePassword(5, new ChangePasswordRequest("wrongPass", "newPass123")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void adminCannotDisableOwnAccount() {
        assertThatThrownBy(() -> userProfileService.updateUserStatus(1, 1, false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void provisionArtistAccount_createsArtistUserAndProfile() {
        Company belift = Company.builder().id(1).name("BELIFT LAB").bio("HYBE label").build();
        Role artistRole = Role.builder().id(2).name("ARTIST").build();
        CreateArtistAccountRequest req = new CreateArtistAccountRequest(
                "moka@belift.kr",
                "Sakai Moka",
                "artist123",
                "MOKA",
                "Member of ILLIT",
                "https://cdn.example.com/moka.jpg",
                1,
                LocalDateTime.of(2024, 3, 25, 0, 0)
        );

        when(userRepository.existsByEmailIgnoreCase("moka@belift.kr")).thenReturn(false);
        when(companyService.requireCompany(1)).thenReturn(belift);
        when(roleRepository.findByNameIgnoreCase("ARTIST")).thenReturn(Optional.of(artistRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(20);
            return u;
        });
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(inv -> {
            ArtistProfile p = inv.getArgument(0);
            p.setUserId(20);
            return p;
        });

        ArtistProfileResponse response = artistProfileService.provisionArtistAccount(req);

        assertThat(response.userId()).isEqualTo(20);
        assertThat(response.stageName()).isEqualTo("MOKA");
        assertThat(response.verified()).isTrue();
        assertThat(response.company().name()).isEqualTo("BELIFT LAB");
    }
}
