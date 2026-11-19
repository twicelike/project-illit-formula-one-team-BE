package project.illit.smedia.common.ratelimit;

public interface RateLimitStore {

    RateLimitResult consume(String key, int limit, int windowSeconds);
}
