package org.ops4j.pax.web.extender.whiteboard.runtime;

import java.util.Arrays;
import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;

/**
 * Default implementation of {@link org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class DefaultWelcomeFileMapping implements WelcomeFileMapping
{

    /**
     * Http Context id.
     */
    private String m_httpContextId;

    /**
     * welcome files
     */
    private String[] m_welcomeFiles;

    /**
     * redirect flag
     * true - send redirect
     * false - use forward
     */
    private boolean m_redirect;

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#getHttpContextId()
     */
    public String getHttpContextId()
    {
        return m_httpContextId;
    }

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#isRedirect()
     */
    public boolean isRedirect()
    {
        return m_redirect;
    }

    /**
     * @see org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping#getWelcomeFiles()
     */
    public String[] getWelcomeFiles()
    {
        return m_welcomeFiles;
    }

    /**
     * Setter.
     *
     * @param httpContextId id of the http context these welcome pages belongs to
     */
    public void setHttpContextId( String httpContextId )
    {
        m_httpContextId = httpContextId;
    }

    /**
     * Setter
     * @param welcomeFiles welcome files
     */
    public void setWelcomeFiles( String[] welcomeFiles )
    {
        m_welcomeFiles = welcomeFiles;
    }

    /**
     * Setter
     * @param redirect weather to redirect or forward.
     */
    public void setRedirect( boolean redirect )
    {
        m_redirect = redirect;
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "httpContextId=" ).append( m_httpContextId )
            .append( ",welcomeFiles=" ).append( Arrays.deepToString( m_welcomeFiles ) )
            .append( ",redirect=" ).append( m_redirect )
            .append( "}" )
            .toString();
    }
}
