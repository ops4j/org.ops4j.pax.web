package org.ops4j.pax.web.service.internal.ng;

import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import javax.servlet.Servlet;
import org.osgi.service.http.HttpContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RegistrationRepositoryImpl implements RegistrationRepository
{

    private static final Log m_logger = LogFactory.getLog( RegistrationRepositoryImpl.class );

    private Map<String, Registration> m_registrations;

    RegistrationRepositoryImpl()
    {
        m_registrations = new HashMap<String, Registration>();
    }
    
    public Collection<Registration> get()
    {
        return m_registrations.values();
    }

    public Registration registerServlet( String alias, Servlet servlet, Dictionary initParams, HttpContext context )
    {
        if( m_logger.isDebugEnabled() )
        {
            m_logger.debug( "Registering Servlet: [" + alias + "] -> " + servlet + " into repository " + this );
        }
        Registration registration = new ServletRegistration( alias, servlet, initParams, context );
        m_registrations.put( registration.getAlias(), registration );
        return registration;
    }

    // TODO handle invalid params on registration (nulls, ...)
    // TODO do not allow duplicate alias registration  within the whole service
    // TODO do not allow duplicate servlet registration within the whole service
}
