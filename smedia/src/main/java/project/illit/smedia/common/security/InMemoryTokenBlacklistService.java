package project.illit.smedia.common.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist for Phase 1.
 * Can be replaced with a Redis-backed implementation in Phase 3 without changing callers.
 */
@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank() || expiresAt == null) {
            return;
        }
        cleanupExpired();
        if (expiresAt.isAfter(Instant.now())) {
            blacklistedTokens.put(jti, expiresAt);
        }
    }

    @Override
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Instant expiresAt = blacklistedTokens.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            blacklistedTokens.remove(jti);
            return false;
        }
        return true;
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
