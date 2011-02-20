/*
 * Copyright 2007 Alin Dreghiciu, Achim Nierbeck.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.ops4j.pax.web.service.spi.WarManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Register/unregister web applications once a bundle containing a "WEB-INF/web.xml" gets started or stopped.
 *
 * @author Alin Dreghiciu
 * @author Achim Nierbeck
 * @author Hiram Chirino <hiram@hiramchirino.com>
 * @since 0.3.0, Decemver 27, 2007
 */
class WebXmlObserver implements BundleObserver<URL>, WarManager
{

    static final String UNDEPLOYED_STATE = "undeployed";
    static final String WAITING_STATE = "waiting";
    static final String DEPLOYED_STATE = "deployed";

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( WebXmlObserver.class );
    /**
     * web.xml parser to be used.
     */
    private final WebXmlParser m_parser;
    /**
     * Web app publisher.
     */
    private final WebAppPublisher m_publisher;

    /**
     * Mapping between the bundle id and WebApp
     */
    private final HashMap<Long, WebApp> webApps = new HashMap<Long, WebApp>();

    /**
     * The queue of published WebApp objects to a context.
     */
    private final HashMap<String, LinkedList<WebApp>> contexts = new HashMap<String, LinkedList<WebApp>>();


    private final BundleContext bundleContext;
    
    private final WebEventDispatcher eventDispatcher;
    
    /**
     * Creates a new web.xml observer.
     *
     * @param parser    parser for web.xml
     * @param publisher web app publisher
     * @param bundleContext 
     *
     * @throws NullArgumentException if parser or publisher is null
     */
    WebXmlObserver( final WebXmlParser parser, final WebAppPublisher publisher, final WebEventDispatcher eventDispatcher, BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( parser, "Web.xml Parser" );
        NullArgumentException.validateNotNull( publisher, "Web App Publisher" );
        NullArgumentException.validateNotNull( eventDispatcher, "WebEvent Dispatcher" );
        NullArgumentException.validateNotNull( bundleContext, "BundleContext" );
        m_parser = parser;
        m_publisher = publisher;
        this.bundleContext = bundleContext;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Parse the web.xml and publish the corresponding web app.
     * The received list is expected to contain one URL of an web.xml (only first is used.
     * The web.xml will be parsed and resulting web application structure will be registered with the http service.
     *
     * @throws NullArgumentException if bundle or list of web xmls is null
     * @throws PreConditionException if the list of web xmls is empty or more then one xml
     * @see BundleObserver#addingEntries(Bundle,List)
     */
    public void addingEntries( final Bundle bundle, final List<URL> entries )
    {
        NullArgumentException.validateNotNull( bundle, "Bundle" );
        NullArgumentException.validateNotNull( entries, "List of web.xml's" );

        //Context name is also needed for some of the pre-condition checks, therefore it is retrieved here.
        String contextName = extractContextName(bundle);
        LOG.info( String.format( "Using [%s] as web application context name", contextName ) );

        // try-catch only to inform framework and listeners of an event.
        try {
	        PreConditionException.validateEqualTo( entries.size(), 1, "Number of xml's" );
	        PreConditionException.validateEqualTo( "WEB-INF".compareToIgnoreCase(Path.getDirectParent(entries.get(0))), 0, "Direct parent of web.xml" );
        } catch (PreConditionException pce) {
        	eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"+contextName, bundle, bundleContext.getBundle(), pce));
        	throw pce;
        }        
        
        if( webApps.containsKey(bundle.getBundleId()) ) {
            LOG.debug(String.format("Already found a web application in bundle %d", bundle.getBundleId()));
            return;
        }

        final URL webXmlURL = entries.get( 0 );
        LOG.debug( "Parsing a web application from [" + webXmlURL + "]" );

        String rootPath = extractRootPath(bundle);
        LOG.info( String.format( "Using [%s] as web application root path", rootPath ) );
        
        InputStream is = null;
        try
        {
            is = webXmlURL.openStream();
            final WebApp webApp = m_parser.parse( is );
            if( webApp != null )
            {
                LOG.debug( "Parsed web app [" + webApp + "]" );
                webApp.setWebXmlURL(webXmlURL);
                webApp.setBundle( bundle );
                webApp.setContextName( contextName );
                webApp.setRootPath( rootPath );
                webApp.setDeploymentState(UNDEPLOYED_STATE);

                webApps.put(bundle.getBundleId(), webApp);

                // The Webapp-Deploy header controls if the app is deployed on
                // startup.
                if( "true".equals(opt(getHeader(bundle, "Webapp-Deploy"), "true"))) {
                    deploy(webApp);
                } else {
                    eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED, "/" + webApp.getContextName(), webApp.getBundle(), bundleContext.getBundle()));
                }

            }
        }
        catch( IOException ignore )
        {
            LOG.error( "Could not parse web.xml", ignore );
            eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"+contextName, bundle, bundleContext.getBundle(), ignore));
		}
        finally
        {
            if( is != null )
            {
                try
                {
                    is.close();
                }
                catch( IOException ignore )
                {
                    // just ignore
                }
            }
        }
    }

    private void deploy(WebApp webApp) {

        Bundle bundle = webApp.getBundle();
        URL webXmlURL = webApp.getWebXmlURL();
        String contextName = webApp.getContextName();

        eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYING, "/" + contextName, bundle, bundleContext.getBundle()));
        if ( !contexts.containsKey(contextName) ) {
            LinkedList<WebApp> queue = new LinkedList<WebApp>();
            contexts.put(contextName, queue);
            queue.add(webApp);

            webApp.setDeploymentState(DEPLOYED_STATE);
            m_publisher.publish(webApp);
            eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYED, "/"+contextName, bundle, bundleContext.getBundle()));
        } else {
            LinkedList<WebApp> queue = contexts.get(contextName);
            queue.add(webApp);

            Collection<Long> duplicateIds = new LinkedList<Long>();
            for (WebApp duplicateWebApp : queue) {
                duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
            }

            webApp.setDeploymentState(WAITING_STATE);
            eventDispatcher.webEvent(new WebEvent(WebEvent.WAITING, "/"+contextName, bundle, bundleContext.getBundle(), duplicateIds ));
        }

    }


	/**
     * Unregisters registered web app once that the bundle that contains the web.xml gets stopped.
     * The list of web.xml's is expected to contain only one entry (only first will be used).
     *
     * @throws NullArgumentException if bundle or list of web xmls is null
     * @throws PreConditionException if the list of web xmls is empty or more then one xml
     * @see BundleObserver#removingEntries(Bundle,List)
     */
    public void removingEntries( final Bundle bundle, final List<URL> entries )
    {
        WebApp webApp = webApps.remove(bundle.getBundleId());
        if( webApp!=null && webApp.getDeploymentState()!=UNDEPLOYED_STATE ) {
            undeploy(webApp);
        }
    }

    private void undeploy(WebApp webApp) {

        String contextName = webApp.getContextName();
        LinkedList<WebApp> queue = contexts.get(contextName);
        if( queue!=null ) {

            // Are we the published web app??
            if( queue.get(0) == webApp ) {

                webApp.setDeploymentState(UNDEPLOYED_STATE);
                eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYING, "/"+ contextName, webApp.getBundle(), bundleContext.getBundle()));
                m_publisher.unpublish( webApp );
                eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED, "/"+ contextName, webApp.getBundle(), bundleContext.getBundle()));
                queue.removeFirst();

                //Below checks if another webapp is waiting for the context, if so the webapp is published.
                LOG.debug("Check for a waiting webapp.");
                if( !queue.isEmpty() ) {
                    LOG.debug("Found another bundle waiting for the context");
                    WebApp next = queue.getFirst();

                    next.setDeploymentState(DEPLOYED_STATE);
                    eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYING, "/"+contextName, next.getBundle(), bundleContext.getBundle()));
                    m_publisher.publish(next);
                    eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYED, "/"+contextName, next.getBundle(), bundleContext.getBundle()));

                } else {
                    contexts.remove(contextName);
                }

            } else if( queue.remove(webApp) ) {
                webApp.setDeploymentState(UNDEPLOYED_STATE);
                eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED, "/"+ contextName, webApp.getBundle(), bundleContext.getBundle()));
            } else {
                LOG.debug("Web application was not in the deployment queue");
            }

        } else {
            LOG.debug(String.format("No web application published under context: %s", contextName));
        }

    }

    public int start(long bundleId, String contextName) {
        WebApp webApp = webApps.get(bundleId);
        if( webApp==null ) {
            return WAR_NOT_FOUND;
        }
        if( webApp.getDeploymentState()!=UNDEPLOYED_STATE ) {
            return ALREADY_STARTED;
        }
        if( contextName!=null ) {
            webApp.setContextName(contextName);
        }
        deploy(webApp);
        return SUCCESS;
    }

    public int stop(long bundleId) {
        WebApp webApp = webApps.get(bundleId);
        if( webApp==null ) {
            return WAR_NOT_FOUND;
        }
        if( webApp.getDeploymentState()==UNDEPLOYED_STATE ) {
            return ALREADY_STOPPED;
        }
        undeploy(webApp);
        return SUCCESS;
    }

    /**
	 * @param bundle
	 * @return
	 */
	private String extractRootPath(final Bundle bundle) {
        String rootPath = getHeader(bundle, "Webapp-Root");
        if( rootPath == null )
        {
        	LOG.debug( "No 'Webapp-Root' manifest attribute specified" );
        	rootPath = "";
        }
        rootPath = stripPrefix(rootPath, "/");
        rootPath = stripSuffix(rootPath, "/");
        rootPath = rootPath.trim();
		return rootPath;
	}

    static private String stripPrefix(String value, String prefix) {
        if( value.startsWith( prefix ) )
        {
        	return value.substring( prefix.length() );
        }
        return value;
    }

    static private <T> T opt(T value, T orElse) {
        if( value== null ) {
            return orElse;
        }
        return value;
    }

    static private String stripSuffix(String value, String suffix) {
        if( value.endsWith( suffix ) )
        {
        	return value.substring(0,  value.length() - suffix.length() );
        }
        return value;
    }

    /**
     * @param bundle
     * @return
     */
    static private String getHeader(final Bundle bundle, String...keys) {
        // Look in the bundle...
        Dictionary headers = bundle.getHeaders();
        for(String key:keys) {
            String value = (String) headers.get(key);
            if( value != null )
            {
                return value;
            }
        }

        // Next, look in the bundle's fragments.
        Bundle[] bundles = bundle.getBundleContext().getBundles();
        for (Bundle fragment : bundles) {
        	//only fragments are in resolved state
            if (fragment.getState() != bundle.RESOLVED) 
                continue;

            headers = fragment.getHeaders();
            for(String key:keys) {
                String value = (String) headers.get(key);
                if( value != null )
                {
                    return value;
                }
            }
        }
        return null;
    }


	/**
	 * @param bundle
	 * @return
	 */
	private String extractContextName(final Bundle bundle) {
		// set the context name as first looking for a manifest entry named Web-ContextPath
        String contextName = getHeader(bundle, "Web-ContextPath", "Webapp-Context");
        // if not found use the old pax Webapp-Context
        if( contextName == null )
        {
            LOG.debug( "No 'Web-ContextPath' or 'Webapp-Context' manifest attribute specified" );
            final String symbolicName = bundle.getSymbolicName();
            if( symbolicName == null )
            {
                contextName = String.valueOf( bundle.getBundleId() );
                LOG.debug( String.format( "Using bundle id [%s] as context name", contextName ) );
            }
            else
            {
                contextName = symbolicName;
                LOG.debug( String.format( "Using bundle symbolic name [%s] as context name", contextName ) );
            }
        }
        contextName = contextName.trim();
        if( contextName.startsWith( "/" ) )
        {
            contextName = contextName.substring( 1 );
        }
		return contextName;
	}

}
