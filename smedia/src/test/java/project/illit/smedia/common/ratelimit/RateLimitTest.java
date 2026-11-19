package project.illit.smedia.common.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import project.illit.smedia.common.exception.TooManyRequestsException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimitTest {

    @Test
    void slidingWindowStore_allowsUpToLimitAndBlocksExcess() {
        InMemorySlidingWindowRateLimitStore store = new InMemorySlidingWindowRateLimitStore();

        RateLimitResult r1 = store.consume("login:ip:127.0.0.1", 2, 60);
        RateLimitResult r2 = store.consume("login:ip:127.0.0.1", 2, 60);
        RateLimitResult r3 = store.consume("login:ip:127.0.0.1", 2, 60);
        RateLimitResult otherIp = store.consume("login:ip:10.0.0.1", 2, 60);

        assertThat(r1.allowed()).isTrue();
        assertThat(r1.remaining()).isEqualTo(1);
        assertThat(r2.allowed()).isTrue();
        assertThat(r2.remaining()).isZero();
        assertThat(r3.allowed()).isFalse();
        assertThat(r3.retryAfterSeconds()).isGreaterThanOrEqualTo(1L);
        assertThat(otherIp.allowed()).isTrue();
    }

    @Test
    void interceptor_setsHeadersAndThrowsTooManyRequestsOnLimitExceeded() throws Exception {
        InMemorySlidingWindowRateLimitStore store = new InMemorySlidingWindowRateLimitStore();
        RateLimitInterceptor interceptor = new RateLimitInterceptor(store, true, 120, 60);

        DummyController controller = new DummyController();
        Method method = DummyController.class.getMethod("limitedEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(controller, method);

        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/test");
        req1.setRemoteAddr("192.168.1.10");
        MockHttpServletResponse res1 = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(req1, res1, handlerMethod)).isTrue();
        assertThat(res1.getHeader(RateLimitInterceptor.HEADER_LIMIT)).isEqualTo("2");
        assertThat(res1.getHeader(RateLimitInterceptor.HEADER_REMAINING)).isEqualTo("1");

        MockHttpServletResponse res2 = new MockHttpServletResponse();
        assertThat(interceptor.preHandle(req1, res2, handlerMethod)).isTrue();
        assertThat(res2.getHeader(RateLimitInterceptor.HEADER_REMAINING)).isEqualTo("0");

        MockHttpServletResponse res3 = new MockHttpServletResponse();
        assertThatThrownBy(() -> interceptor.preHandle(req1, res3, handlerMethod))
                .isInstanceOf(TooManyRequestsException.class);
        assertThat(res3.getHeader("Retry-After")).isNotBlank();
    }

    static class DummyController {
        @RateLimit(limit = 2, windowSeconds = 60, keyType = RateLimit.KeyType.IP)
        public void limitedEndpoint() {
        }
    }
}
