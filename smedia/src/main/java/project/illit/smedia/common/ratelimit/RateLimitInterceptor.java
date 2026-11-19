package project.illit.smedia.common.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import project.illit.smedia.common.exception.TooManyRequestsException;
import project.illit.smedia.common.security.CustomUserDetails;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String HEADER_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_REMAINING = "X-RateLimit-Remaining";

    private final RateLimitStore rateLimitStore;
    private final boolean enabled;
    private final int defaultLimit;
    private final int defaultWindowSeconds;

    public RateLimitInterceptor(
            RateLimitStore rateLimitStore,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.default-limit:120}") int defaultLimit,
            @Value("${app.rate-limit.default-window-seconds:60}") int defaultWindowSeconds
    ) {
        this.rateLimitStore = rateLimitStore;
        this.enabled = enabled;
        this.defaultLimit = defaultLimit;
        this.defaultWindowSeconds = defaultWindowSeconds;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!enabled || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        RateLimit annotation = resolveAnnotation(handlerMethod);
        int limit = annotation != null ? annotation.limit() : defaultLimit;
        int windowSeconds = annotation != null ? annotation.windowSeconds() : defaultWindowSeconds;
        RateLimit.KeyType keyType = annotation != null ? annotation.keyType() : RateLimit.KeyType.USER_OR_IP;

        String bucketName = resolveBucketName(handlerMethod, annotation);
        String subjectKey = resolveSubjectKey(request, keyType);
        String fullKey = bucketName + ":" + subjectKey;

        RateLimitResult result = rateLimitStore.consume(fullKey, limit, windowSeconds);
        response.setHeader(HEADER_LIMIT, String.valueOf(result.limit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(result.remaining()));

        if (!result.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(result.retryAfterSeconds()));
            throw new TooManyRequestsException(
                    "Too many requests. Please retry after " + result.retryAfterSeconds() + " seconds.",
                    result.retryAfterSeconds()
            );
        }

        return true;
    }

    private RateLimit resolveAnnotation(HandlerMethod handlerMethod) {
        RateLimit methodAnnotation = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RateLimit.class);
    }

    private String resolveBucketName(HandlerMethod handlerMethod, RateLimit annotation) {
        if (annotation != null && !annotation.name().isBlank()) {
            return annotation.name();
        }
        return handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
    }

    private String resolveSubjectKey(HttpServletRequest request, RateLimit.KeyType keyType) {
        if (keyType == RateLimit.KeyType.USER_OR_IP) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails user) {
                return "user:" + user.getId();
            }
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}
