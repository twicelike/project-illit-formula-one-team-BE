package project.illit.smedia.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TooManyRequestsException extends AppException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}
