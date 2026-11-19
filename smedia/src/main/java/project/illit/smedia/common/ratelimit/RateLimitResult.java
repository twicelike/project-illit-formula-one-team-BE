package project.illit.smedia.common.ratelimit;

public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long retryAfterSeconds
) {
}
