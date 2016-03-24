/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    
    public static <T> boolean assertTrue(Supplier<T> actual, Predicate<T> expected) {
        notNull(actual);
        notNull(expected);

        return expected.test(actual.get());
    }

    public static <T> void assertThat(String message, T actual, Predicate<T> expected) {
        assertThat(message, (Supplier<T>) () -> actual, expected);
    }
    
    
    public static <T> boolean assertTrue(T actual, Predicate<T> expected) {
        return assertTrue((Supplier<T>) () -> actual, expected);
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
