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

import java.io.IOException;
import java.net.URL;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Context for HTTP Requests.
 *
 * <p>
 * This service defines methods that the Http Service may call to get
 * information for a request.
 *
 * <p>
 * Servlets may be associated with an {@code HttpContext} service. Servlets that
 * are associated using the same {@code HttpContext} object will share the same
 * {@code ServletContext} object.
 *
 * <p>
 * If no {@code HttpContext} service is associated, a default
 * {@code HttpContext} is used. The behavior of the methods on the default
 * {@code HttpContext} is defined as follows:
 * <ul>
 * <li>{@code getMimeType} - Does not define any customized MIME types for the
 * {@code Content-Type} header in the response, and always returns {@code null}.
 * </li>
 * <li>{@code handleSecurity} - Performs implementation-defined authentication
 * on the request.</li>
 * <li>{@code getResource} - Assumes the named resource is in the bundle of the
 * servlet service. This method calls the servlet bundle's
 * {@code Bundle.getResource} method, and returns the appropriate URL to access
 * the resource. On a Java runtime environment that supports permissions, the
 * Http Service needs to be granted
 * {@code org.osgi.framework.AdminPermission[*,RESOURCE]}.</li>
 * </ul>
 *
 * @author $Id: ab5459d16e836e60dca639d9e073f95ebccad271 $
 */
public interface HttpContext {

    /**
     * {@code HttpServletRequest} attribute specifying the name of the
     * authenticated user. The value of the attribute can be retrieved by
     * {@code HttpServletRequest.getRemoteUser}. This attribute name is
     * {@code org.osgi.service.http.authentication.remote.user}.
     *
     * @since 1.1
     */
    String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";

    /**
     * {@code HttpServletRequest} attribute specifying the scheme used in
     * authentication. The value of the attribute can be retrieved by
     * {@code HttpServletRequest.getAuthType}. This attribute name is
     * {@code org.osgi.service.http.authentication.type}.
     *
     * @since 1.1
     */
    String AUTHENTICATION_TYPE = "org.osgi.service.http.authentication.type";

    /**
     * {@code HttpServletRequest} attribute specifying the {@code Authorization}
     * object obtained from the {@code org.osgi.service.useradmin.UserAdmin}
     * service. The value of the attribute can be retrieved by
     * {@code HttpServletRequest.getAttribute(HttpContext.AUTHORIZATION)}. This
     * attribute name is {@code org.osgi.service.useradmin.authorization}.
     *
     * @since 1.1
     */
    String AUTHORIZATION = "org.osgi.service.useradmin.authorization";

    /**
     * Handles security for the specified request.
     *
     * <p>
     * The Http Service calls this method prior to servicing the specified
     * request. This method controls whether the request is processed in the
     * normal manner or an error is returned.
     *
     * <p>
     * If the request requires authentication and the Authorization header in
     * the request is missing or not acceptable, then this method should set the
     * WWW-Authenticate header in the response object, set the status in the
     * response object to Unauthorized(401) and return {@code false}. See also
     * RFC 2617: <i>HTTP Authentication: Basic and Digest Access Authentication
     * </i> (available at <a href="http://www.ietf.org/rfc/rfc2617.txt">RFC 2617</a>).
     *
     * <p>
     * If the request requires a secure connection and the {@code getScheme}
     * method in the request does not return 'https' or some other acceptable
     * secure protocol, then this method should set the status in the response
     * object to Forbidden(403) and return {@code false}.
     *
     * <p>
     * When this method returns {@code false}, the Http Service will send the
     * response back to the client, thereby completing the request. When this
     * method returns {@code true}, the Http Service will proceed with servicing
     * the request.
     *
     * <p>
     * If the specified request has been authenticated, this method must set the
     * {@link #AUTHENTICATION_TYPE} request attribute to the type of
     * authentication used, and the {@link #REMOTE_USER} request attribute to
     * the remote user (request attributes are set using the
     * {@code setAttribute} method on the request). If this method does not
     * perform any authentication, it must not set these attributes.
     *
     * <p>
     * If the authenticated user is also authorized to access certain resources,
     * this method must set the {@link #AUTHORIZATION} request attribute to the
     * {@code Authorization} object obtained from the
     * {@code org.osgi.service.useradmin.UserAdmin} service.
     *
     * <p>
     * The servlet responsible for servicing the specified request determines
     * the authentication type and remote user by calling the
     * {@code getAuthType} and {@code getRemoteUser} methods, respectively, on
     * the request.
     *
     * @param request  The HTTP request.
     * @param response The HTTP response.
     * @return {@code true} if the request should be serviced, {@code false} if
     * the request should not be serviced and Http Service will send the
     * response back to the client.
     * @throws java.io.IOException may be thrown by this method. If this occurs,
     *                             the Http Service will terminate the request and close the socket.
     */
    boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException;

    /**
     * Maps a resource name to a URL.
     *
     * <p>
     * Called by the Http Service to map a resource name to a URL. For servlet
     * registrations, Http Service will call this method to support the
     * {@code ServletContext} methods {@code getResource} and
     * {@code getResourceAsStream}. For resource registrations, Http Service
     * will call this method to locate the named resource. The context can
     * control from where resources come. For example, the resource can be
     * mapped to a file in the bundle's persistent storage area via
     * {@code bundleContext.getDataFile(name).toURL()} or to a resource in the
     * context's bundle via {@code getClass().getResource(name)}
     *
     * @param name the name of the requested resource
     * @return URL that Http Service can use to read the resource or
     * {@code null} if the resource does not exist.
     */
    URL getResource(String name);

    /**
     * Maps a name to a MIME type.
     *
     * <p>
     * Called by the Http Service to determine the MIME type for the specified
     * name. For servlets, the Http Service will call this method to support the
     * {@code ServletContext} method {@code getMimeType}. For resources, the
     * Http Service will call this method to determine the MIME type for the
     * {@code Content-Type} header in the response.
     *
     * @param name The name for which to determine the MIME type.
     * @return The MIME type (e.g. text/html) of the specified name or
     * {@code null} to indicate that the Http Service should determine
     * the MIME type itself.
     */
    String getMimeType(String name);

}
