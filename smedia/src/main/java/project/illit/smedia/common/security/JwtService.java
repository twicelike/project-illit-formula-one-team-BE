package project.illit.smedia.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import project.illit.smedia.user.entity.User;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties, TokenBlacklistService tokenBlacklistService) {
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getAccessTokenExpirationMs());
        String roleName = user.getRole() != null ? user.getRole().getName() : "FAN";

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, roleName)
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtProperties.getRefreshTokenExpirationMs());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getEmail())
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
            if (!expectedTokenType.equals(tokenType)) {
                return false;
            }
            if (claims.getExpiration() == null || claims.getExpiration().toInstant().isBefore(Instant.now())) {
                return false;
            }
            return !tokenBlacklistService.isBlacklisted(claims.getId());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            Claims claims = parseClaims(token);
            if (claims.getId() != null && claims.getExpiration() != null) {
                tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // Token is already malformed or expired; nothing left to revoke.
        }
    }

    public String extractEmail(Claims claims) {
        return claims.getSubject();
    }

    public Integer extractUserId(Claims claims) {
        return claims.get(CLAIM_USER_ID, Integer.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.getAccessTokenExpirationMs() / 1000L;
    }
}
