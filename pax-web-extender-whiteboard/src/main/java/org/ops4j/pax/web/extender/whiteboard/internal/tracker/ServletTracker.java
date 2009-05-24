/*
 * Copyright 2007 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.extender.whiteboard.internal.tracker;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ops4j.pax.web.extender.whiteboard.ExtenderConstants;
import org.ops4j.pax.web.extender.whiteboard.internal.ExtenderContext;
import org.ops4j.pax.web.extender.whiteboard.internal.element.ServletWebElement;
import org.ops4j.pax.web.extender.whiteboard.runtime.DefaultServletMapping;
import org.ops4j.pax.web.service.WebContainerConstants;

/**
 * Tracks {@link Servlet}s.
 *
 * @author Alin Dreghiciu
 * @author Thomas Joseph
 * @since 0.4.0, April 05, 2008
 */
public class ServletTracker
    extends AbstractTracker<Servlet, ServletWebElement>
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( ServletTracker.class );

    /**
     * Constructor.
     *
     * @param extenderContext extender context; cannot be null
     * @param bundleContext   extender bundle context; cannot be null
     */
    public ServletTracker( final ExtenderContext extenderContext,
                           final BundleContext bundleContext )
    {
        super(
            extenderContext,
            bundleContext,
            Servlet.class, HttpServlet.class
        );
    }

    /**
     * @see AbstractTracker#createWebElement(ServiceReference, Object)
     */
    @Override
    ServletWebElement createWebElement(
        final ServiceReference serviceReference,
        final Servlet published )
    {
        final Object alias = serviceReference.getProperty( ExtenderConstants.PROPERTY_ALIAS );
        final Object urlPatternsProp = serviceReference.getProperty( ExtenderConstants.PROPERTY_URL_PATTERNS );
        final String[] initParamKeys = serviceReference.getPropertyKeys();
        final Object servletName = serviceReference.getProperty(WebContainerConstants.SERVLET_NAME);
        if( servletName != null
        		&& ( !(servletName instanceof String)
        				|| servletName.toString().trim().length() == 0)) {
            LOG.warn( "Registered servlet [" + published + "] did not contain a valid servlet-name property.");
            return null;
        }
        if( alias != null && urlPatternsProp != null )
        {
            LOG.warn( "Registered servlet [" + published + "] cannot have both alias and url patterns" );
            return null;
        }
        if( alias == null && urlPatternsProp == null )
        {
            LOG.warn(
                "Registered servlet [" + published + "] did not contain a valid alias or url patterns property"
            );
            return null;
        }
        if( alias != null
            && ( !( alias instanceof String )
                 || ( (String) alias ).trim().length() == 0 ) )
        {
            LOG.warn( "Registered servlet [" + published + "] did not contain a valid alias property" );
            return null;
        }
        String[] urlPatterns = null;
        if( urlPatternsProp != null )
        {
            if( urlPatternsProp instanceof String
                && ( (String) urlPatternsProp ).trim().length() != 0 )
            {
                urlPatterns = new String[]{ (String) urlPatternsProp };
            }
            else if( urlPatternsProp instanceof String[] )
            {
                urlPatterns = (String[]) urlPatternsProp;
            }
            else
            {
                LOG.warn(
                    "Registered servlet [" + published
                    + "] has an invalid url pattern property (must be a non empty String or String[])"
                );
                return null;
            }
        }
        Object httpContextId = serviceReference.getProperty( ExtenderConstants.PROPERTY_HTTP_CONTEXT_ID );
        if( httpContextId != null && ( !( httpContextId instanceof String )
                                       || ( (String) httpContextId ).trim().length() == 0 ) )
        {
            LOG.warn( "Registered servlet [" + published + "] did not contain a valid http context id" );
            return null;
        }
        // make all the service parameters available as initParams to registering the Servlet
        Map<String, String> initParams = new HashMap<String, String>();
        for(String key: initParamKeys) {
            try {
                String value = serviceReference.getProperty(key)==null ? "":serviceReference.getProperty(key).toString();
                initParams.put(key, value);
            } catch (Exception ignore) {
                // ignore
            }
        }
        DefaultServletMapping mapping = new DefaultServletMapping();
        mapping.setHttpContextId( (String) httpContextId );
        mapping.setServlet( published );
        if(servletName != null) 
        {
          mapping.setServletName(servletName.toString().trim());
        }
        mapping.setAlias( (String) alias );
        mapping.setUrlPatterns( urlPatterns );
        mapping.setInitParams(initParams);
        return new ServletWebElement( mapping );
    }

}
