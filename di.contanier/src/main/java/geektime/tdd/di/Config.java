package geektime.tdd.di;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

public interface Config {
    @Documented
    @Retention(RUNTIME)
    @Target({ElementType.FIELD})
    @interface Export {
        Class<?> value();
    }

    @Documented
    @Retention(RUNTIME)
    @Target({ElementType.FIELD})
    @interface Static {}
}
