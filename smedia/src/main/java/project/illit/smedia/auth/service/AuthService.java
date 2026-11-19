package project.illit.smedia.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.auth.dto.AuthResponse;
import project.illit.smedia.auth.dto.AuthUserSummary;
import project.illit.smedia.auth.dto.LoginRequest;
import project.illit.smedia.auth.dto.LogoutRequest;
import project.illit.smedia.auth.dto.RefreshTokenRequest;
import project.illit.smedia.auth.dto.RegisterRequest;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ForbiddenException;
import project.illit.smedia.common.exception.UnauthorizedException;
import project.illit.smedia.common.security.JwtService;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.RoleRepository;
import project.illit.smedia.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered");
        }

        Role fanRole = roleRepository.findByNameIgnoreCase(RoleName.FAN.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.FAN.name()).build()));

        User user = User.builder()
                .role(fanRole)
                .email(normalizedEmail)
                .name(request.getName().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .avatar(request.getAvatar())
                .totalFollower(0)
                .totalFollowing(0)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        MemberProfile memberProfile = MemberProfile.builder()
                .user(savedUser)
                .membershipTier("STANDARD")
                .totalLoyaltyPoints(0)
                .build();
        memberProfileRepository.save(memberProfile);

        return buildAuthResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("Account is disabled");
        }

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken().trim();
        if (!jwtService.isTokenValid(refreshToken, JwtService.TOKEN_TYPE_REFRESH)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Claims claims = jwtService.parseClaims(refreshToken);
        String email = jwtService.extractEmail(claims);

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("User associated with token no longer exists"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("Account is disabled");
        }

        // Rotate refresh token so old one cannot be reused
        jwtService.revokeToken(refreshToken);
        return buildAuthResponse(user);
    }

    public void logout(String authorizationHeader, LogoutRequest request) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String accessToken = authorizationHeader.substring(7).trim();
            jwtService.revokeToken(accessToken);
        }
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            jwtService.revokeToken(request.getRefreshToken().trim());
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(AuthUserSummary.from(user))
                .build();
    }
}
