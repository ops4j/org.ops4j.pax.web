package org.ops4j.pax.web.extender.whiteboard.internal.element;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping;
import org.ops4j.pax.web.extender.whiteboard.internal.util.WebContainerUtils;
import org.ops4j.pax.web.service.WebContainer;

/**
 * Registers/unregisters {@link org.ops4j.pax.web.extender.whiteboard.ErrorPageMapping} with {@link WebContainer}.
 *
 * @author dsklyut
 * @since 0.7.0
 */
public class ErrorPageWebElement implements WebElement
{

    private final ErrorPageMapping m_errorPageMapping;

    /**
     * Constructor.
     *
     * @param errorPageMapping error page mapping; cannot be null
     */
    public ErrorPageWebElement( final ErrorPageMapping errorPageMapping )
    {
        NullArgumentException.validateNotNull( errorPageMapping, "error page errorPageMapping" );
        this.m_errorPageMapping = errorPageMapping;
    }

    /**
     * registers error page
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
                .registerErrorPage( m_errorPageMapping.getError(), m_errorPageMapping.getLocation(), httpContext );
        }
        else
        {
            throw new UnsupportedOperationException(
                "Internal error: In use HttpService is not an WebContainer (from Pax Web)"
            );
        }
    }

    /**
     * unregisters error page
     *
     * @param httpService
     * @param httpContext
     */
    public void unregister( HttpService httpService, HttpContext httpContext )
    {
        if( WebContainerUtils.isWebContainer( httpService ) )
        {
            ( (WebContainer) httpService ).unregisterErrorPage( m_errorPageMapping.getError(), httpContext );
        }
    }

    public String getHttpContextId()
    {
        return m_errorPageMapping.getHttpContextId();
    }

    @Override
    public String toString()
    {
        return new StringBuffer()
            .append( this.getClass().getSimpleName() )
            .append( "{" )
            .append( "mapping=" ).append( m_errorPageMapping )
            .append( "}" )
            .toString();
    }
}
