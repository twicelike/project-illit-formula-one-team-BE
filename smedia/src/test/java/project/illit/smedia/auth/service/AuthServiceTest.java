package project.illit.smedia.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import project.illit.smedia.auth.dto.AuthResponse;
import project.illit.smedia.auth.dto.LoginRequest;
import project.illit.smedia.auth.dto.LogoutRequest;
import project.illit.smedia.auth.dto.RefreshTokenRequest;
import project.illit.smedia.auth.dto.RegisterRequest;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.UnauthorizedException;
import project.illit.smedia.common.security.InMemoryTokenBlacklistService;
import project.illit.smedia.common.security.JwtProperties;
import project.illit.smedia.common.security.JwtService;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.RoleRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private MemberProfileRepository memberProfileRepository;
    @Mock
    private AuthenticationManager authenticationManager;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtProperties properties = new JwtProperties();
        properties.setSecret("c21lZGlhLXdlbGNvbWUtdG8taWxsaXQtZmFuZG9tLXNvY2lhbC1wbGF0Zm9ybS1zZWNyZXQta2V5LTIwMjY=");
        properties.setAccessTokenExpirationMs(900_000L);
        properties.setRefreshTokenExpirationMs(604_800_000L);
        jwtService = new JwtService(properties, new InMemoryTokenBlacklistService());

        authService = new AuthService(
                userRepository,
                roleRepository,
                memberProfileRepository,
                passwordEncoder,
                authenticationManager,
                jwtService
        );
    }

    @Test
    void register_createsFanUserWithEncodedPasswordAndMemberProfile() {
        RegisterRequest request = RegisterRequest.builder()
                .email("NewFan@Example.com")
                .name("New Fan")
                .password("secret123")
                .build();

        Role fanRole = Role.builder().id(3).name("FAN").build();
        when(userRepository.existsByEmailIgnoreCase("newfan@example.com")).thenReturn(false);
        when(roleRepository.findByNameIgnoreCase("FAN")).thenReturn(Optional.of(fanRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10);
            return u;
        });

        AuthResponse response = authService.register(request);

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("newfan@example.com");
        assertThat(response.getUser().getRole()).isEqualTo("FAN");
        assertThat(response.getUser().isVerified()).isFalse();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(passwordEncoder.matches("secret123", userCaptor.getValue().getPassword())).isTrue();

        ArgumentCaptor<MemberProfile> profileCaptor = ArgumentCaptor.forClass(MemberProfile.class);
        verify(memberProfileRepository).save(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getMembershipTier()).isEqualTo("STANDARD");
        assertThat(profileCaptor.getValue().getTotalLoyaltyPoints()).isZero();
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        RegisterRequest request = RegisterRequest.builder()
                .email("taken@example.com")
                .name("Taken")
                .password("secret123")
                .build();
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void login_refresh_and_logout_lifecycle() {
        User artist = User.builder()
                .id(2)
                .email("yunah@belift.kr")
                .name("Noh Yunah")
                .password(passwordEncoder.encode("password123"))
                .role(Role.builder().id(2).name("ARTIST").build())
                .isActive(true)
                .build();

        when(userRepository.findByEmailIgnoreCase("yunah@belift.kr")).thenReturn(Optional.of(artist));

        AuthResponse loginRes = authService.login(new LoginRequest("yunah@belift.kr", "password123"));
        assertThat(loginRes.getUser().isVerified()).isTrue();
        assertThat(jwtService.isTokenValid(loginRes.getAccessToken(), JwtService.TOKEN_TYPE_ACCESS)).isTrue();

        // Refresh rotates token and invalidates the old refresh token
        AuthResponse refreshed = authService.refresh(new RefreshTokenRequest(loginRes.getRefreshToken()));
        assertThat(jwtService.isTokenValid(loginRes.getRefreshToken(), JwtService.TOKEN_TYPE_REFRESH)).isFalse();
        assertThat(jwtService.isTokenValid(refreshed.getAccessToken(), JwtService.TOKEN_TYPE_ACCESS)).isTrue();

        // Using old refresh token again must fail
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(loginRes.getRefreshToken())))
                .isInstanceOf(UnauthorizedException.class);

        // Logout revokes both active access and refresh tokens
        authService.logout("Bearer " + refreshed.getAccessToken(), new LogoutRequest(refreshed.getRefreshToken()));
        assertThat(jwtService.isTokenValid(refreshed.getAccessToken(), JwtService.TOKEN_TYPE_ACCESS)).isFalse();
        assertThat(jwtService.isTokenValid(refreshed.getRefreshToken(), JwtService.TOKEN_TYPE_REFRESH)).isFalse();
    }
}
