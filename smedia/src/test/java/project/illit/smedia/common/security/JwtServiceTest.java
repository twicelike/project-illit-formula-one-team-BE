package project.illit.smedia.common.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("c21lZGlhLXdlbGNvbWUtdG8taWxsaXQtZmFuZG9tLXNvY2lhbC1wbGF0Zm9ybS1zZWNyZXQta2V5LTIwMjY=");
        properties.setAccessTokenExpirationMs(900_000L);
        properties.setRefreshTokenExpirationMs(604_800_000L);

        jwtService = new JwtService(properties, new InMemoryTokenBlacklistService());
        sampleUser = User.builder()
                .id(5)
                .email("gllit.minh@gmail.com")
                .name("Minh Nguyen")
                .role(Role.builder().id(3).name("FAN").build())
                .isActive(true)
                .build();
    }

    @Test
    void generateAccessToken_containsExpectedClaimsAndValidatesOnlyAsAccess() {
        String token = jwtService.generateAccessToken(sampleUser);

        assertThat(jwtService.isTokenValid(token, JwtService.TOKEN_TYPE_ACCESS)).isTrue();
        assertThat(jwtService.isTokenValid(token, JwtService.TOKEN_TYPE_REFRESH)).isFalse();

        Claims claims = jwtService.parseClaims(token);
        assertThat(jwtService.extractEmail(claims)).isEqualTo("gllit.minh@gmail.com");
        assertThat(jwtService.extractUserId(claims)).isEqualTo(5);
        assertThat(claims.get("role", String.class)).isEqualTo("FAN");
    }

    @Test
    void generateRefreshToken_validatesOnlyAsRefreshAndCanBeRevoked() {
        String refreshToken = jwtService.generateRefreshToken(sampleUser);

        assertThat(jwtService.isTokenValid(refreshToken, JwtService.TOKEN_TYPE_REFRESH)).isTrue();
        assertThat(jwtService.isTokenValid(refreshToken, JwtService.TOKEN_TYPE_ACCESS)).isFalse();

        jwtService.revokeToken(refreshToken);
        assertThat(jwtService.isTokenValid(refreshToken, JwtService.TOKEN_TYPE_REFRESH)).isFalse();
    }
}
