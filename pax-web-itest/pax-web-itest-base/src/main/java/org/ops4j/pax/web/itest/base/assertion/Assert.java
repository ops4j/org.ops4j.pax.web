package org.ops4j.pax.web.itest.base.assertion;

import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Using functional assertions instead of Hamcrest
 */
public class Assert {

    public static <T> void assertThat(Supplier<T> actual, Predicate<T> expected) {
        assertThat("Test failed", actual, expected);
    }

    public static <T> void assertThat(String message, Supplier<T> actual, Predicate<T> expected) {
        notNull(actual);
        notNull(expected);

        if (!expected.test(actual.get())) {
            throw new AssertionError(message);
        }
    }

    public static <T> void assertThat(String message, T actual, Predicate<T> expected) {
        assertThat(message, (Supplier<T>) () -> actual, expected);
    }

    private static void notNull(Object object, String message) {
        if (message == null) {
            notNull(object);
        } else if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }


    private static void notNull(Object object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }
}
