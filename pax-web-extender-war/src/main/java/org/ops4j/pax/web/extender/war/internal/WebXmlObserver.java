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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.ops4j.pax.web.extender.war.internal.extender.Extension;
import org.ops4j.pax.web.extender.war.internal.parser.dom.DOMWebXmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.Path;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.ops4j.pax.web.service.spi.WebEvent.DEPLOYED;
import static org.ops4j.pax.web.service.spi.WebEvent.DEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.FAILED;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYED;
import static org.ops4j.pax.web.service.spi.WebEvent.UNDEPLOYING;
import static org.ops4j.pax.web.service.spi.WebEvent.WAITING;

/**
 * Register/unregister web applications once a bundle containing a "WEB-INF/web.xml" gets started or stopped.
 *
 * @author Alin Dreghiciu
 * @author Achim Nierbeck
 * @author Hiram Chirino <hiram@hiramchirino.com>
 * @author Marc Klinger - mklinger[at]nightlabs[dot]de
 * @author Guillaume Nodet
 * @since 0.3.0, Decemver 27, 2007
 */
class WebXmlObserver implements WarManager
{

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger( WebXmlObserver.class );
    /**
     * Web app publisher.
     */
    private final WebAppPublisher m_publisher;

    /**
     * Mapping between the bundle id and WebApp
     */
    private final Map<Long, WebApp> webApps = new HashMap<Long, WebApp>();

    /**
     * The queue of published WebApp objects to a context.
     */
    private final Map<String, List<WebApp>> contexts = new HashMap<String, List<WebApp>>();


    private final BundleContext bundleContext;
    
    private final WebEventDispatcher eventDispatcher;
    
    /**
     * Creates a new web.xml observer.
     *
     * @param publisher web app publisher
     * @param bundleContext 
     *
     * @throws NullArgumentException if parser or publisher is null
     */
    WebXmlObserver( final WebAppPublisher publisher, final WebEventDispatcher eventDispatcher, BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( publisher, "Web App Publisher" );
        NullArgumentException.validateNotNull( eventDispatcher, "WebEvent Dispatcher" );
        NullArgumentException.validateNotNull( bundleContext, "BundleContext" );
        m_publisher = publisher;
        this.bundleContext = bundleContext;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * Create the extension managed by the extender for the given bundle
     * or null if this bundle is not to be managed
     *
     * @param bundle
     * @return the extension
     * @throws Exception
     */
    public Extension createExtension(final Bundle bundle) throws Exception {
        // Find root path
        String path = extractRootPath(bundle);
        if (path.isEmpty()) {
            path = "WEB-INF";
        } else {
            path = path + "/WEB-INF";
        }
        // Find web xml
        URL webXmlURL = bundle.getEntry(path + "/web.xml");
        if (webXmlURL == null) {
            return null;
        }
        // Look for jetty web xml
        URL jettyWebXmlURL = null;
        Enumeration<URL> enums = bundle.findEntries(path, "*web*.xml", false);
        while (enums != null && enums.hasMoreElements()) {
            URL url = enums.nextElement();
            if (isJettyWebXml(url)) {
                if (jettyWebXmlURL == null) {
                    jettyWebXmlURL = url;
                } else {
                    throw new IllegalArgumentException("Found multiple jetty web xml descriptors. Aborting");
                }
            }
        }
        // Parse web xml
        InputStream is = null;
        String contextName = "unknown";
        try
        {
            //Context name is also needed for some of the pre-condition checks, therefore it is retrieved here.
            contextName = extractContextName(bundle);
            LOG.info( String.format( "Using [%s] as web application context name", contextName ) );

            List<String> virtualHostList = extractVirtualHostList(bundle);
            LOG.info( String.format( "[%d] virtual hosts defined in bundle header", virtualHostList.size()));

            List<String> connectorList = extractConnectorList(bundle);
            LOG.info( String.format( "[%d] connectors defined in bundle header", connectorList.size()));

            is = webXmlURL.openStream();
            final WebApp webApp = new DOMWebXmlParser().parse(bundle, is);
            if( webApp != null )
            {
                LOG.debug( "Parsed web app [" + webApp + "]" );
                
                webApp.setWebXmlURL(webXmlURL);
                webApp.setJettyWebXmlURL(jettyWebXmlURL);
                webApp.setVirtualHostList(virtualHostList);
                webApp.setConnectorList(connectorList);
                webApp.setBundle( bundle );
                webApp.setContextName( contextName );
                webApp.setRootPath( extractRootPath(bundle) );
                webApp.setDeploymentState(UNDEPLOYED);
                // Register the web app
                synchronized (webApps) {
                    webApps.put(bundle.getBundleId(), webApp);
                }
                return new Extension() {
                    public void start() {
                        // Check if the web app has already been destroyed
                        synchronized (webApps) {
                            if (!webApps.containsKey(bundle.getBundleId())) {
                                return;
                            }
                        }
                        deploy(webApp);
                    }
                    public void destroy() {
                        // Flag this web app has destroyed by removing it
                        // from the list
                        synchronized (webApps) {
                            webApps.remove(bundle.getBundleId());
                        }
                        undeploy(webApp);
                    }
                };
            }
            return null;
        } catch (Exception e) {
            webEvent(FAILED, contextName, bundle, e);
            throw e;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private boolean isJettyWebXml(URL url) {
    	String path = url.getPath();
    	path = path.substring(path.lastIndexOf('/')+1);
    	boolean match = path.matches("jetty[0-9]?-web\\.xml");
    	if (match)
    		return match;
    	match = path.matches("web-jetty\\.xml");
		return match;
	}
    
    public void deploy(WebApp webApp) {
        webEvent(DEPLOYING, webApp);
        List<WebApp> queue = getQueue(webApp);
        synchronized (queue) {
            if (queue.isEmpty()) {
                queue.add(webApp);
                m_publisher.publish(webApp, eventDispatcher, bundleContext);
            } else {
                queue.add(webApp);
                Collection<Long> duplicateIds = new LinkedList<Long>();
                for (WebApp duplicateWebApp : queue) {
                    duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
                }
                webApp.setDeploymentState(WAITING);
                webEvent(WAITING, webApp, duplicateIds);
            }
        }
    }


    public void undeploy(WebApp webApp) {
        // Are we the published web app??
        List<WebApp> queue = getQueue(webApp);
        synchronized (queue) {
            if( !queue.isEmpty() && queue.get(0) == webApp ) {

                webApp.setDeploymentState(UNDEPLOYED);
                webEvent(UNDEPLOYING, webApp);
                m_publisher.unpublish(webApp);
                webEvent(UNDEPLOYED, webApp);
                queue.remove(0);

                //Below checks if another webapp is waiting for the context, if so the webapp is published.
                LOG.debug("Check for a waiting webapp.");
                if( !queue.isEmpty() ) {
                    LOG.debug("Found another bundle waiting for the context");
                    WebApp next = queue.get(0);

                    next.setDeploymentState(DEPLOYED);
                    webEvent(DEPLOYING, next);
                    m_publisher.publish(next, eventDispatcher, bundleContext);
                } else {
                    synchronized (contexts) {
                        contexts.remove(webApp.getContextName());
                    }
                }
            } else if( queue.remove(webApp) ) {
                webApp.setDeploymentState(UNDEPLOYED);
                webEvent(UNDEPLOYED, webApp);
            } else {
                LOG.debug("Web application was not in the deployment queue");
            }
        }
    }

    private List<WebApp> getQueue(WebApp webApp) {
        synchronized (contexts) {
            List<WebApp> queue = contexts.get(webApp.getContextName());
            if (queue == null) {
                queue = new LinkedList<WebApp>();
                contexts.put(webApp.getContextName(), queue);
            }
            return queue;
        }
    }


    public int start(long bundleId, String contextName) {
        WebApp webApp;
        synchronized (webApps) {
            webApp = webApps.get(bundleId);
        }
        if( webApp == null ) {
            return WAR_NOT_FOUND;
        }
        if( webApp.getDeploymentState() != UNDEPLOYED ) {
            return ALREADY_STARTED;
        }
        if( contextName!=null ) {
            webApp.setContextName(contextName);
        }
        deploy(webApp);
        return SUCCESS;
    }

    public int stop(long bundleId) {
        WebApp webApp;
        synchronized (webApps) {
            webApp = webApps.get(bundleId);
        }
        if( webApp == null ) {
            return WAR_NOT_FOUND;
        }
        if( webApp.getDeploymentState() == UNDEPLOYED ) {
            return ALREADY_STOPPED;
        }
        undeploy(webApp);
        return SUCCESS;
    }

    private void webEvent(int type, WebApp webApp) {
        webEvent(type, webApp.getContextName(), webApp.getBundle(), null);
    }

    private void webEvent(int type, WebApp webApp, Collection<Long> ids) {
        eventDispatcher.webEvent(new WebEvent(type, "/" + webApp.getContextName(), webApp.getBundle(), bundleContext.getBundle(), ids));
    }

    private void webEvent(int type, String contextName, Bundle webAppBundle, Throwable t) {
        eventDispatcher.webEvent(new WebEvent(type, "/" + contextName, webAppBundle, bundleContext.getBundle(), t));
    }


    /**
	 * @param bundle
	 * @return
	 */
	private String extractRootPath(final Bundle bundle) {
        String rootPath = getHeader(bundle, "Webapp-Root");
        if( rootPath == null )
        {
        	LOG.trace( "No 'Webapp-Root' manifest attribute specified in bundle {}", bundle );
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

            // A fragment must also have the FRAGMENT_HOST header and the FRAGMENT_HOST header
            // must be equal to the bundle symbolic name
            String fragmentHost = (String) fragment.getHeaders().get(Constants.FRAGMENT_HOST);
            if ((fragmentHost == null) || (!fragmentHost.equals(bundle.getSymbolicName()))) {
            	continue;
            }
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
	
	private List<String> extractVirtualHostList(final Bundle bundle) {
		List<String> virtualHostList = new LinkedList<String>();
		String virtualHostListAsString = getHeader(bundle,"Web-VirtualHosts");
		if ((virtualHostListAsString != null) && (virtualHostListAsString.length() > 0)){
			String[] virtualHostArray = virtualHostListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				virtualHostList.add(virtualHost.trim());
			}
		}
		return virtualHostList;
	}
	
	private List<String> extractConnectorList(final Bundle bundle) {
		List<String> connectorList = new LinkedList<String>();
		String connectorListAsString = getHeader(bundle,"Web-Connectors");
		if ((connectorListAsString != null) && (connectorListAsString.length() > 0)){
			String[] virtualHostArray = connectorListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				connectorList.add(virtualHost.trim());
			}
		}
		return connectorList;
	}
}
