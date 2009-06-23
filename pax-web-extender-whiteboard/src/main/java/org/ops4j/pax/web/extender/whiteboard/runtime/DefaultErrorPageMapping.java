package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;
import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;

/**
 * Default implementation of {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping}
 */
public class DefaultErrorPageMapping implements ErrorPageMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;

    /**
     * Error code or fqn of Exception
     */
    private String m_error;

    /**
     * Location of error page
     */
    private String m_location;

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getError()
     */
    public String getError()
    {
        return m_error;
    }

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping#getLocation()
     */
    public String getLocation()
    {
        return m_location;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context this error page belongs to
     */
    public void setHttpContextId( String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter
     *
     * @param error code or FQN of Exception class
     */
    public void setError( String error )
    {
        m_error = error;
    }

    /**
     * Setter
     *
     * @param location location of error page
     */
    public void setLocation( String location )
    {
        m_location = location;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",error=" ).append( m_error )
            .append( ",location=" ).append( m_location )
            .append( "}" )
            .toString();
    }
}
