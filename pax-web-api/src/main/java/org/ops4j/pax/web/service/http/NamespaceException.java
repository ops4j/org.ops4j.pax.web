/*
 * Copyright 2023 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ops4j.pax.web.service.http;

/**
 * A NamespaceException is thrown to indicate an error with the caller's request
 * to register a servlet or resources into the URI namespace of the Http
 * Service. This exception indicates that the requested alias already is in use.
 *
 * @author $Id: 850fca5cacf0c39a585431f9abfd1ed163473848 $
 */
public class NamespaceException extends Exception {

    private static final long serialVersionUID = 7235606031147877747L;

    /**
     * Construct a {@code NamespaceException} object with a detail message.
     *
     * @param message the detail message
     */
    public NamespaceException(String message) {
        super(message);
    }

    /**
     * Construct a {@code NamespaceException} object with a detail message and a
     * nested exception.
     *
     * @param message The detail message.
     * @param cause   The nested exception.
     */
    public NamespaceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the nested exception.
     *
     * <p>
     * This method predates the general purpose exception chaining mechanism.
     * The {@code getCause()} method is now the preferred means of obtaining
     * this information.
     *
     * @return The result of calling {@code getCause()}.
     */
    public Throwable getException() {
        return getCause();
    }

    /**
     * Returns the cause of this exception or {@code null} if no cause was set.
     *
     * @return The cause of this exception or {@code null} if no cause was set.
     * @since 1.2
     */
    @Override
    public Throwable getCause() {
        return super.getCause();
    }

    /**
     * Initializes the cause of this exception to the specified value.
     *
     * @param cause The cause of this exception.
     * @return This exception.
     * @throws IllegalArgumentException If the specified cause is this
     *                                  exception.
     * @throws IllegalStateException    If the cause of this exception has already
     *                                  been set.
     * @since 1.2
     */
    @Override
    public Throwable initCause(Throwable cause) {
        return super.initCause(cause);
    }

}
