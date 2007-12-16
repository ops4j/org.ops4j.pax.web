/*  Copyright 2007 Alin Dreghiciu.
 *
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
package org.ops4j.pax.web.service.internal;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Request;
import org.osgi.service.http.HttpContext;

/**
 * An HttServletRequestWrapper that handles HttpServiceAuthentication attributes.
 *
 * @author Alin Dreghiciu
 * @since December 10, 1007
 */
public class HttpServiceRequestWrapper extends HttpServletRequestWrapper
{

    private static final Log LOG = LogFactory.getLog( HttpServiceRequestWrapper.class );

    private Request m_request = null;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request original request to be wrapped
     *
     * @throws IllegalArgumentException if the request is null
     */
    public HttpServiceRequestWrapper( final HttpServletRequest request )
    {
        super( request );
        // try to cast to a jetty servlet request. It may fail if a filter will change the request when calling handle
        // in the chain
        try
        {
            m_request = (Request) request;
        }
        catch( ClassCastException ignore )
        {
            LOG.warn(
                "Could not cast the request to jetty Request, so the http service authentication related features will "
                + "be disabled. Current request is of class [" + request.getClass().getName() + "]"
            );
        }
    }

    /**
     * Filter the setting of authentication related attributes.
     * If one of HttpContext.AUTHENTICATION_TYPE or HTTPContext.REMOTE_USER set the corresponding values in original
     * request.
     *
     * @see javax.servlet.http.HttpServletRequest#setAttribute(String, Object)
     */
    @Override
    public void setAttribute( final String name, Object value )
    {
        if( HttpContext.AUTHENTICATION_TYPE.equals( name ) )
        {
            handleAuthenticationType( value );
        }
        else if( HttpContext.REMOTE_USER.equals( name ) )
        {
            handleRemoteUser( value );
        }
        super.setAttribute( name, value );
    }

    /**
     * Handles setting of authentication type attribute.
     *
     * @param authenticationType new authentication type
     */
    private void handleAuthenticationType( final Object authenticationType )
    {
        if( !isJettyRequestAvailable() )
        {
            return;
        }
        if( authenticationType != null )
        {
            // be defensive
            if( !( authenticationType instanceof String ) )
            {
                final String message = "Attribute " + HttpContext.AUTHENTICATION_TYPE
                                       + " expected to be a String but was an [" + authenticationType.getClass() + "]";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
        }
        m_request.setAuthType( (String) authenticationType );
    }

    /**
     * Handles setting of remote user attribute.
     *
     * @param remoteUser new remote user name
     */
    private void handleRemoteUser( final Object remoteUser )
    {
        if( !isJettyRequestAvailable() )
        {
            return;
        }
        Principal userPrincipal = null;
        if( remoteUser != null )
        {
            // be defensive
            if( !( remoteUser instanceof String ) )
            {
                final String message = "Attribute " + HttpContext.REMOTE_USER + " expected to be a String but was an ["
                                       + remoteUser.getClass() + "]";
                LOG.error( message );
                throw new IllegalArgumentException( message );
            }
            userPrincipal = new User( (String) remoteUser );
        }
        m_request.setUserPrincipal( userPrincipal );
    }

    /**
     * Returns true if the wraped request is a Jetty Request.
     *
     * @return true if the wraped request is a Jetty Request
     */
    private boolean isJettyRequestAvailable()
    {
        if( m_request == null )
        {
            LOG.warn(
                "HttpService authentication handling is currently disabled (most probably because the request is not a Jetty request. Setting this attribute has no effect"
            );
        }
        return m_request != null;
    }

    /**
     * A simple Principal.
     */
    private static class User implements Principal
    {

        /**
         * principla's name.
         */
        private final String m_name;

        /**
         * Creates a new user principal.
         * The name must be not null.
         *
         * @param name user's name
         */
        public User( final String name )
        {
            Assert.notNull( "User name", name );
            m_name = name;
        }

        /**
         * @see java.security.Principal#getName()
         */
        public String getName()
        {
            return m_name;
        }

        /**
         * @see java.security.Principal#hashCode()
         */
        @Override
        public int hashCode()
        {
            return m_name.hashCode();
        }

        /**
         * @see java.security.Principal#equals(Object)
         */
        @Override
        public boolean equals( final Object other )
        {
            if( other == null || !( other instanceof User ) )
            {
                return false;
            }
            final User otherAsUser = (User) other;
            return m_name.equals( otherAsUser.m_name );
        }

        /**
         * @see java.security.Principal#toString()
         */
        @Override
        public String toString()
        {
            return m_name;
        }
    }

}
