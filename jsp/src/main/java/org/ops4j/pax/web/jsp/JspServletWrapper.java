/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.jsp;

import java.io.IOException;
import java.util.concurrent.Callable;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jasper.servlet.JspServlet;
import org.osgi.framework.Bundle;
import org.ops4j.pax.swissbox.lang.ContextClassLoaderUtils;
import org.ops4j.pax.web.jsp.internal.JasperClassLoader;

/**
 * Wrapper of Jasper JspServlet that knows how to deal with resources loaded from osgi context.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, January 07, 2008
 */
public class JspServletWrapper
    implements Servlet
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( JspServletWrapper.class );
    /**
     * Jasper Servlet.
     */
    private final JspServlet m_jasperServlet;
    /**
     * Jasper specific class loader.
     */
    private final JasperClassLoader m_jasperClassLoader;

    public JspServletWrapper( final Bundle bundle )
    {
        m_jasperServlet = new JspServlet();
        m_jasperClassLoader = new JasperClassLoader( bundle, JasperClassLoader.class.getClassLoader() );
    }

    /**
     * Delegates to jasper servlet with a controlled context class loader.
     *
     * @see JspServlet#init(ServletConfig)
     */
    public void init( final ServletConfig config )
        throws ServletException
    {
        try
        {
            ContextClassLoaderUtils.doWithClassLoader(
                m_jasperClassLoader,
                new Callable<Void>()
                {

                    public Void call()
                        throws Exception
                    {
                        m_jasperServlet.init( config );
                        return null;
                    }

                }
            );
        }
        catch( ServletException e )
        {
            // re-thrown
            throw e;
        }
        catch( RuntimeException e )
        {
            // re-thrown
            throw e;
        }
        catch( Exception ignore )
        {
            // ignored as it should never happen
            LOG.error( "Ignored exception", ignore );
        }
    }

    /**
     * Delegates to jasper servlet.
     *
     * @see JspServlet#getServletConfig()
     */
    public ServletConfig getServletConfig()
    {
        return m_jasperServlet.getServletConfig();
    }

    /**
     * Delegates to jasper servlet with a controlled context class loader.
     *
     * @see JspServlet#service(ServletRequest, ServletResponse)
     */
    public void service( final ServletRequest req, final ServletResponse res )
        throws ServletException, IOException
    {
        try
        {
            ContextClassLoaderUtils.doWithClassLoader(
                m_jasperClassLoader,
                new Callable<Void>()
                {

                    public Void call()
                        throws Exception
                    {
                        m_jasperServlet.service( req, res );
                        return null;
                    }

                }
            );
        }
        catch( ServletException e )
        {
            // re-thrown
            throw e;
        }
        catch( IOException e )
        {
            // re-thrown
            throw e;
        }
        catch( RuntimeException e )
        {
            // re-thrown
            throw e;
        }
        catch( Exception ignore )
        {
            // ignored as it should never happen
            LOG.error( "Ignored exception", ignore );
        }
    }

    /**
     * Delegates to jasper servlet.
     *
     * @see JspServlet#getServletInfo()
     */
    public String getServletInfo()
    {
        return m_jasperServlet.getServletInfo();
    }

    /**
     * Delegates to jasper servlet with a controlled context class loader.
     *
     * @see JspServlet#destroy()
     */
    public void destroy()
    {
        try
        {
            ContextClassLoaderUtils.doWithClassLoader(
                m_jasperClassLoader,
                new Callable<Void>()
                {

                    public Void call()
                        throws Exception
                    {
                        m_jasperServlet.destroy();
                        return null;
                    }

                }
            );
        }
        catch( RuntimeException e )
        {
            // re-thrown
            throw e;
        }
        catch( Exception ignore )
        {
            // ignored as it should never happen
            LOG.error( "Ignored exception", ignore );
        }
    }
}
