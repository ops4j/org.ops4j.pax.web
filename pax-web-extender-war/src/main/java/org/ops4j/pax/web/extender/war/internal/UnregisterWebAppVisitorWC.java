/*
 * Copyright 2007 Alin Dreghiciu.
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
package org.ops4j.pax.web.extender.war.internal;

import java.util.EventListener;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppConstraintMapping;
import org.ops4j.pax.web.extender.war.internal.model.WebAppErrorPage;
import org.ops4j.pax.web.extender.war.internal.model.WebAppFilter;
import org.ops4j.pax.web.extender.war.internal.model.WebAppListener;
import org.ops4j.pax.web.extender.war.internal.model.WebAppLoginConfig;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServlet;
import org.ops4j.pax.web.extender.war.internal.model.WebAppServletContainerInitializer;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A visitor that unregisters a web application.
 * Cannot be reused, it has to be one per visit.
 */
class UnregisterWebAppVisitorWC
    implements WebAppVisitor
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( UnregisterWebAppVisitorWC.class );
    /**
     * HttpService to be used for registration.
     */
    private final WebContainer m_webContainer;
    /**
     * Http context used during registration.
     */
    private HttpContext m_httpContext;

    /**
     * Creates a new unregistration visitor.
     *
     * @param webContainer http service to be used for unregistration. Cannot be null.
     *
     * @throws NullArgumentException if web container is null
     */
    UnregisterWebAppVisitorWC( final WebContainer webContainer )
    {
        NullArgumentException.validateNotNull( webContainer, "Web Container" );
        m_webContainer = webContainer;
    }

    /**
     * Unregisters resources related to web app.
     *
     * @see WebAppVisitor#visit(org.ops4j.pax.web.extender.war.internal.model.WebApp)
     */
    public void visit( final WebApp webApp )
    {
        m_httpContext = webApp.getHttpContext();
        // unregister war content resources
        try
        {
            m_webContainer.unregister( "/" );
        }
        catch( Exception ignore )
        {
            LOG.error( "Unregistration exception. Skipping.", ignore );
        }
        // unregister welcome files
        try
        {
            m_webContainer.unregisterWelcomeFiles(
                m_httpContext
            );
        }
        catch( Exception ignore )
        {
            LOG.error( "Unregistration exception. Skipping.", ignore );
        }
        // unregister JSP support
        try
        {
            m_webContainer.unregisterJsps(
                m_httpContext
            );
        }
        catch( UnsupportedOperationException ignore )
        {
            LOG.warn( ignore.getMessage() );
        }
        catch( Exception ignore )
        {
            LOG.error( "Unregistration exception. Skipping.", ignore );
        }
    }

    /**
     * Unregisters servlet from web container.
     *
     * @throws NullArgumentException if servlet is null
     * @see WebAppVisitor#visit(WebAppServlet)
     */
    public void visit( final WebAppServlet webAppServlet )
    {
        NullArgumentException.validateNotNull( webAppServlet, "Web app servlet" );
        final Class<? extends Servlet> servletClass = webAppServlet.getServletClass();
        if( servletClass != null )
        {
            try
            {
                m_webContainer.unregisterServlets( servletClass );
                webAppServlet.setServletClass(null);
            }
            catch( Exception ignore )
            {
                LOG.error( "Unregistration exception. Skipping.", ignore );
            }
        }
    }

    /**
     * Unregisters filter from web container.
     *
     * @throws NullArgumentException if filter is null
     * @see WebAppVisitor#visit(WebAppFilter)
     */
    public void visit( final WebAppFilter webAppFilter )
    {
        NullArgumentException.validateNotNull( webAppFilter, "Web app filter" );
        final Filter filter = webAppFilter.getFilter();
        if( filter != null )
        {
            try
            {
                m_webContainer.unregisterFilter( filter );
            }
            catch( Exception ignore )
            {
                LOG.error( "Unregistration exception. Skipping.", ignore );
            }
        }
    }

    /**
     * Unregisters listeners from web container.
     *
     * @throws NullArgumentException if listener is null
     * @see WebAppVisitor#visit(WebAppListener)
     */
    public void visit( final WebAppListener webAppListener )
    {
        NullArgumentException.validateNotNull( webAppListener, "Web app listener" );
        final EventListener listener = webAppListener.getListener();
        if( listener != null )
        {
            try
            {
                m_webContainer.unregisterEventListener( listener );
            }
            catch( Exception ignore )
            {
                LOG.error( "Unregistration exception. Skipping.", ignore );
            }
        }
    }

    /**
     * Unregisters error pages from web container.
     *
     * @throws NullArgumentException if error page is null
     * @see WebAppVisitor#visit(WebAppErrorPage)
     */
    public void visit( final WebAppErrorPage webAppErrorPage )
    {
        NullArgumentException.validateNotNull( webAppErrorPage, "Web app error page" );
        try
        {
            m_webContainer.unregisterErrorPage( webAppErrorPage.getError(), m_httpContext );
        }
        catch( Exception ignore )
        {
            LOG.error( "Unregistration exception. Skipping.", ignore );
        }
    }

	public void visit(WebAppLoginConfig loginConfig) {
		NullArgumentException.validateNotNull( loginConfig, "Web app login config" );
		m_webContainer.unregisterLoginConfig(m_httpContext);
	}

	public void visit(WebAppConstraintMapping constraintMapping) {
		NullArgumentException.validateNotNull( constraintMapping, "Web app constraint mapping" );
		m_webContainer.unregisterConstraintMapping(m_httpContext);
	}

	public void visit(
			WebAppServletContainerInitializer servletContainerInitializer) {
		NullArgumentException.validateNotNull( servletContainerInitializer, "Servlet Container Initializer" );
		m_webContainer.unregisterServletContainerInitializer(m_httpContext);
	}

    public void end() {
    }
}
