package org.ops4j.pax.web.extender.samples.whiteboard.internal;

import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

/**
 * Servlet that either throws and exception if errorException request parameter is set and is a correct FQN of
 * Throwable subtype.
 * or sends an error if errorCode request parameter is provided.
 *
 * Used to test error page registration.
 *
 * errorCode takes precedence or errorException.
 *
 * If neither is provided or errorCode is not valid http error code or errorException is not correct
 * will throw an IllegalArgumentException.
 * errorCode must in in 400 or 500 range to be considered valid.
 */
public class ExceptionServlet extends HttpServlet
{

    /**
     * 
     */
    private static final long serialVersionUID = -58844579506172515L;
	
    private final Set<Integer> VALID_ERROR_CODES = new HashSet<Integer>()
    {
    	/**
    	 * 
    	 */
        private static final long serialVersionUID = -5608318022683417716L;

	{

            add( HttpServletResponse.SC_BAD_REQUEST );
            add( HttpServletResponse.SC_UNAUTHORIZED );
            add( HttpServletResponse.SC_FORBIDDEN );
            add( HttpServletResponse.SC_NOT_FOUND );
            add( HttpServletResponse.SC_METHOD_NOT_ALLOWED );
            add( HttpServletResponse.SC_NOT_ACCEPTABLE );
            add( HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED );
            add( HttpServletResponse.SC_REQUEST_TIMEOUT );
            add( HttpServletResponse.SC_CONFLICT );
            add( HttpServletResponse.SC_GONE );
            add( HttpServletResponse.SC_LENGTH_REQUIRED );
            add( HttpServletResponse.SC_PRECONDITION_FAILED );
            add( HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE );
            add( HttpServletResponse.SC_REQUEST_URI_TOO_LONG );
            add( HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE );
            add( HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE );
            add( HttpServletResponse.SC_EXPECTATION_FAILED );
            add( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            add( HttpServletResponse.SC_NOT_IMPLEMENTED );
            add( HttpServletResponse.SC_BAD_GATEWAY );
            add( HttpServletResponse.SC_SERVICE_UNAVAILABLE );
            add( HttpServletResponse.SC_GATEWAY_TIMEOUT );
            add( HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED );
        }};

    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp )
        throws ServletException, IOException
    {
        String error = req.getParameter( "errorCode" );
        if( error != null && error.trim().length() > 0 )
        {
            int errorCode = -1;
            try
            {
                errorCode = Integer.parseInt( error.trim() );
                if( VALID_ERROR_CODES.contains( errorCode ) )
                {
                    resp.sendError( errorCode, " Echo errorCode " + error );
                    return;
                }
            } catch( NumberFormatException e )
            {
                // ignore
            }
        }
        error = req.getParameter( "errorException" );
        if( error != null && error.trim().length() > 0 )
        {
            try
            {
                Class<?> exp = this.getClass().getClassLoader().loadClass( error.trim() );
                if( Throwable.class.isAssignableFrom( exp ) )
                {
                    throw new ServletException( "Rethrowing " + error, (Throwable) exp.newInstance() );
                }
            } catch( Exception ex )
            {
                // ignore class not found
            }
        }

        throw new IllegalArgumentException( "Just throwing IllegalArgumentException" );
    }
}
