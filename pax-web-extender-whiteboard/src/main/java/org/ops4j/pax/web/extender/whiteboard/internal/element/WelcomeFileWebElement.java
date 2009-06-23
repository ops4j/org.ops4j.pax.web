package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.osgi.service.http.HttpService;
import org.osgi.service.http.HttpContext;
import org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;
import org.ops4j.lang.NullArgumentException;

/**
 * Registers/unregisters {@link org.ops4j.pax.web.extender.whiteboard.WelcomeFileMapping}
 * with {@link org.ops4j.pax.web.service.WebContainer}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class WelcomeFileWebElement implements WebElement
{

    /**
     * welcome file mapping
     */
    private final WelcomeFileMapping m_welcomeFileMapping;

    /**
     * Constructor.
     *
     * @param welcomeFileMapping welcome file mapping; cannot be null
     */
    public WelcomeFileWebElement( WelcomeFileMapping welcomeFileMapping )
    {
        NullArgumentException.validateNotNull( welcomeFileMapping, "Welcome file mapping" );
        this.m_welcomeFileMapping = welcomeFileMapping;
    }

    /**
     * registers welcome file with httpService
     *
     * @param httpService
     * @param httpContext
     */
    public void register( HttpService httpService, HttpContext httpContext )
        throws Exception
    {
        if( WebContainerUtils.isWebContainer( httpService ) )
        {
            ( (WebContainer) httpService )
                .registerWelcomeFiles( m_welcomeFileMapping.getWelcomeFiles(),
                                       m_welcomeFileMapping.isRedirect(),
                                       httpContext
                );
        }
        else
        {
            throw new UnsupportedOperationException(
                "Internal error: In use HttpService is not an WebContainer (from Pax Web)"
            );
        }
    }

    /**
     * unregisters welcome file
     *
     * @param httpService
     * @param httpContext
     */
    public void unregister( HttpService httpService, HttpContext httpContext )
    {
        if( WebContainerUtils.isWebContainer( httpService ) )
        {
            ( (WebContainer) httpService ).unregisterWelcomeFiles( httpContext );
        }
    }

    public String getHttpContextId()
    {
        return m_welcomeFileMapping.getHttpContextId();
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "mapping=" ).append( m_welcomeFileMapping )
            .append( "}" )
            .toString();
    }
}
