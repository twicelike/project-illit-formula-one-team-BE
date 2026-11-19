package project.illit.smedia.common.security;

import java.time.Instant;

public interface TokenBlacklistService {

    void blacklist(String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);
}
