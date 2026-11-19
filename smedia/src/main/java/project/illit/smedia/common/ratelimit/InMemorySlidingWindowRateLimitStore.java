package project.illit.smedia.common.ratelimit;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe sliding-window in-memory rate limiter.
 * Implements RateLimitStore so it can be swapped to Redis in Phase 3 without changing interceptors or controllers.
 */
@Service
public class InMemorySlidingWindowRateLimitStore implements RateLimitStore {

    private static final int CLEANUP_INTERVAL = 500;
    private static final long MAX_STALE_AGE_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, WindowBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    @Override
    public RateLimitResult consume(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        long windowMs = windowSeconds * 1000L;

        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            evictStaleBuckets(now);
        }

        WindowBucket bucket = buckets.computeIfAbsent(key, ignored -> new WindowBucket());
        synchronized (bucket) {
            long cutoff = now - windowMs;
            while (!bucket.timestamps.isEmpty() && bucket.timestamps.peekFirst() <= cutoff) {
                bucket.timestamps.pollFirst();
            }
            bucket.lastAccessedAt = now;

            if (bucket.timestamps.size() >= limit) {
                long oldestTs = bucket.timestamps.peekFirst() != null ? bucket.timestamps.peekFirst() : now;
                long retryAfterMs = Math.max(1000L, (oldestTs + windowMs) - now);
                long retryAfterSeconds = (long) Math.ceil(retryAfterMs / 1000.0);
                return new RateLimitResult(false, limit, 0, retryAfterSeconds);
            }

            bucket.timestamps.addLast(now);
            int remaining = Math.max(0, limit - bucket.timestamps.size());
            return new RateLimitResult(true, limit, remaining, 0);
        }
    }

    private void evictStaleBuckets(long now) {
        buckets.entrySet().removeIf(entry -> {
            WindowBucket bucket = entry.getValue();
            synchronized (bucket) {
                return (now - bucket.lastAccessedAt) > MAX_STALE_AGE_MS && bucket.timestamps.isEmpty();
            }
        });
    }

    private static final class WindowBucket {
        private final Deque<Long> timestamps = new ArrayDeque<>();
        private long lastAccessedAt = System.currentTimeMillis();
    }
}
