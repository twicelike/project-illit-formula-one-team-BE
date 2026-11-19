package project.illit.smedia.common.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    int limit();

    int windowSeconds() default 60;

    KeyType keyType() default KeyType.USER_OR_IP;

    String name() default "";

    enum KeyType {
        IP,
        USER_OR_IP
    }
}
