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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.ops4j.lang.NullArgumentException;
import org.ops4j.lang.PreConditionException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;

/**
 * Register/unregister web applications once a bundle containing a "WEB-INF/web.xml" gets started or stopped.
 *
 * @author Alin Dreghiciu
 * @since 0.3.0, Decemver 27, 2007
 */
class WebXmlObserver
    implements BundleObserver<URL>
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WebXmlObserver.class );
    /**
     * web.xml parser to be used.
     */
    private final WebXmlParser m_parser;
    /**
     * Web app publisher.
     */
    private final WebAppPublisher m_publisher;
    /**
     * Mapping between the URL of web.xml and the published web app.
     */
    private final Map<URI, WebApp> m_publishedWebApps;

    /**
     * reference to the event admin service this might be null. 
     */
    private EventAdmin eventAdminService = null;
    
    /**
     * reference to the log service this might be null. 
     */
    private LogService logService = null;
    
    private final Map<String, Map<URI, WebApp>> waitingWebApps;
    
    private final BundleContext bundleContext;
    
    /**
     * Creates a new web.xml observer.
     *
     * @param parser    parser for web.xml
     * @param publisher web app publisher
     * @param bundleContext 
     *
     * @throws NullArgumentException if parser or publisher is null
     */
    WebXmlObserver( final WebXmlParser parser, final WebAppPublisher publisher, BundleContext bundleContext )
    {
        NullArgumentException.validateNotNull( parser, "Web.xml Parser" );
        NullArgumentException.validateNotNull( publisher, "Web App Publisher" );
        NullArgumentException.validateNotNull( bundleContext, "BundleContext" );
        m_parser = parser;
        m_publisher = publisher;
        m_publishedWebApps = new HashMap<URI, WebApp>();
        waitingWebApps = new HashMap<String, Map<URI,WebApp>>();
        this.bundleContext = bundleContext;
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
        PreConditionException.validateEqualTo( entries.size(), 1, "Number of xml's" );

        final URL webXmlURL = entries.get( 0 );
        LOG.debug( "Parsing a web application from [" + webXmlURL + "]" );
        fireEvent("org/osgi/service/web/DEPLOYING", bundle);
        InputStream is = null;
        try
        {
            is = webXmlURL.openStream();
            final WebApp webApp = m_parser.parse( is );
            if( webApp != null )
            {
                LOG.debug( "Parsed web app [" + webApp + "]" );
                webApp.setBundle( bundle );
                // set the context name as first looking for a manifest entry named Webapp-Context
                // if not set use bundle symbolic name
                String contextName = (String) bundle.getHeaders().get( "Web-ContextPath" );
                if( contextName == null )
                {
                    contextName = (String) bundle.getHeaders().get( "Webapp-Context" );
                }
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

                LOG.info( String.format( "Using [%s] as web application context name", contextName ) );

                
                webApp.setContextName( contextName );
                
                WebApp alreadyPublished;
                
                if ((alreadyPublished = checkAlreadyPublishedContext(contextName)) == null) {
                
	                doPublish(webXmlURL.toURI(), webApp);
	                
	                fireEvent("org/osgi/service/web/DEPLOYED", bundle);
                } else {
                	Map<URI, WebApp> webAppsQueue;
                	if (waitingWebApps.containsKey(contextName)) {
                		webAppsQueue = waitingWebApps.get(contextName);
                	} else {
                		webAppsQueue = new HashMap<URI, WebApp>();
                		waitingWebApps.put(contextName, webAppsQueue);
                	}
                	
                	
                	Collection<WebApp> webApps = webAppsQueue.values();
                	Long[] duplicateIds = new Long[webApps.size()+1];
                	int count = 0;
                	for (WebApp duplicateWebApp : webApps) {
						duplicateIds[count] = duplicateWebApp.getBundle().getBundleId();
						count++;
					}
                	
                	duplicateIds[count] = alreadyPublished.getBundle().getBundleId();
                	
                	webAppsQueue.put(webXmlURL.toURI(), webApp);
                	
                	fireFailedEvent(bundle, null, contextName, duplicateIds);
                }
            }
        }
        catch( IOException ignore )
        {
            LOG.error( "Could not parse web.xml", ignore );
            fireFailedEvent(bundle, ignore, null);
        } catch (URISyntaxException ignore) {
			LOG.error( "Couldn't transform URL to URI ", ignore);
            fireFailedEvent(bundle, ignore, null);
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

	/**
	 * @param webXmlURL
	 * @param webApp
	 * @throws URISyntaxException
	 */
	private void doPublish(final URI webXmlURI, final WebApp webApp)
			throws URISyntaxException {
		m_publisher.publish( webApp );	                
		m_publishedWebApps.put( webXmlURI, webApp );
	}
    
    private WebApp checkAlreadyPublishedContext(String contextName) {
		Collection<WebApp> values = m_publishedWebApps.values();
		for (WebApp webApp : values) {
			WebAppInitParam[] webAppInitParams = webApp.getContextParams();
			for (WebAppInitParam webAppInitParam : webAppInitParams) {
				if (webAppInitParam.getParamName().equalsIgnoreCase("webapp.context")) {
					//webapp context found, now check if this already registered context name matches the new one.
					if (webAppInitParam.getParamValue().equalsIgnoreCase(contextName)) {
						return webApp; //return this webapp since it is the one we are looking for.
					}						
				}
			}
		}
		return null;
	}

	private void fireFailedEvent(Bundle bundle, Exception exception, String context, Long ... ids) {
    	String faileEvent = "org/osgi/service/web/FAILED";
    	Dictionary<String, Object> failedProperties = new Hashtable<String, Object>();
    	if (exception != null)
    		failedProperties.put("exception",exception);
    	if (context != null) {
    		failedProperties.put("collision", context);
    		failedProperties.put("collision.bundles", ids);
    	}
    	fireEvent(faileEvent, bundle, failedProperties);
    }

    private void fireEvent(String topic, Bundle bundle) {
    	fireEvent(topic, bundle, null);
    }
    
    private void fireEvent(String topic, Bundle bundle, Dictionary<String, Object> failure) {
    	if (eventAdminService != null) {
	    	Dictionary<String, Object> properties = new Hashtable<String, Object>();
	    	properties.put("bundle.symbolicName", bundle.getSymbolicName());
	    	properties.put("bundle.id", bundle.getBundleId());
	    	properties.put("bundle", bundle);
	    	properties.put("bundle.version", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
	    	String webContextPath = (String) bundle.getHeaders().get("Web-ContextPath");
	    	if (webContextPath == null || webContextPath.length() == 0)
	    		webContextPath = (String) bundle.getHeaders().get("Webapp-Context"); //PAX fallback
	    	
	    	properties.put("context.path", webContextPath);
	    	properties.put("timestamp", System.currentTimeMillis());
	    	properties.put("extender.bundle", bundleContext.getBundle() );
	    	properties.put("extender.bundle.id", bundleContext.getBundle().getBundleId());
	    	properties.put("extender.bundle.symbolicName", bundleContext.getBundle().getSymbolicName());
	    	properties.put("extender.bundle.version", bundleContext.getBundle().getHeaders().get(Constants.BUNDLE_VERSION));
	    	
	    	if (failure != null) {
	    		Enumeration<String> keys = failure.keys();
	    		while(keys.hasMoreElements()) {
	    			String key = keys.nextElement();
	    			properties.put(key, failure.get(key));
	    		}
	    	}
	    	
	    	Event event = new Event(topic, properties);
	        this.eventAdminService.postEvent(event);
    	} 
    	
    	if (logService != null) {
    		this.logService.log(LogService.LOG_DEBUG, topic);
    	}
	}

	/**
     * Unregisters registered web app once that the bundle that contains the web.xml gets stopped.
     * The list of xb.xml's is expected to contain only one entry (only first will be used).
     *
     * @throws NullArgumentException if bundle or list of web xmls is null
     * @throws PreConditionException if the list of web xmls is empty or more then one xml
     * @see BundleObserver#removingEntries(Bundle,List)
     */
    public void removingEntries( final Bundle bundle, final List<URL> entries )
    {
        NullArgumentException.validateNotNull( bundle, "Bundle" );
        NullArgumentException.validateNotNull( entries, "List of web.xml's" );
        PreConditionException.validateEqualTo( 1, entries.size(), "Number of xml's" );

        final URL webXmlURL = entries.get( 0 );
        LOG.debug( "Unregistering web application parsed from [" + webXmlURL + "]" );
        WebApp toUnpublish = null;
		try {
			toUnpublish = m_publishedWebApps.remove( webXmlURL.toURI() );
	        if( toUnpublish != null )
	        {
	        	fireEvent("org/osgi/service/web/UNDEPLOYING", bundle, null);
	            m_publisher.unpublish( toUnpublish );
	            fireEvent("org/osgi/service/web/UNDEPLOYED", bundle, null);
	            
	            //Below checks if another webapp is waiting for the context, if so the webapp is published. 
	            
	            LOG.debug("Check for a waiting webapp.");
	            String unPublishedContext = null;
	            WebAppInitParam[] webAppInitParams = toUnpublish.getContextParams();
				for (WebAppInitParam webAppInitParam : webAppInitParams) {
					if (webAppInitParam.getParamName().equalsIgnoreCase("webapp.context")) {
						unPublishedContext = webAppInitParam.getParamValue();
						break;
					}
				}
	            
				if (waitingWebApps.containsKey(unPublishedContext)) {
					LOG.debug("Found another bundle waiting for the context");
					Map<URI, WebApp> waitingQueue = waitingWebApps.get(unPublishedContext);
					Set<URI> keySet = waitingQueue.keySet();
					if (!keySet.isEmpty()) {
						URI webXmlURI = keySet.iterator().next();
						WebApp webApp = waitingQueue.remove(webXmlURI);
						LOG.debug("Registering the waiting bundle for the webapp.context");
						
						fireEvent("org/osgi/service/web/DEPLOYING", webApp.getBundle());
						doPublish(webXmlURI, webApp);
						fireEvent("org/osgi/service/web/DEPLOYED", webApp.getBundle());
					}
				}
				

	            
	        } else {
	        	//OK, this might be a duplicate context waiting which needs to be removed, lets see.
	        	if (!waitingWebApps.isEmpty()) {
	        		Set<String> keySet = waitingWebApps.keySet();
	        		String removeKey = null;
	        		for (String key : keySet) {
						Map<URI, WebApp> waitingQueue = waitingWebApps.get(key);
						if (waitingQueue.containsKey(webXmlURL.toURI())){
							//found it, now take care of it
							waitingQueue.remove(webXmlURL.toURI());
							if (waitingQueue.isEmpty()) {
								removeKey = key;
							}
							break;
						}
					}
	        		if (removeKey != null) {
	        			waitingWebApps.remove(removeKey);
	        		}
	        		fireEvent("org/osgi/service/web/UNDEPLOYED", bundle, null);
	        	}
	        }
		} catch (URISyntaxException ignore) {
			LOG.error( String.format("Removing webapp with URL: [%s] failed ", webXmlURL), ignore);
		}
    }

	public void setEventAdminService(Object eventService) {
		this.eventAdminService = (EventAdmin) eventService;
	}

	public void setLogService(Object logService) {
		this.logService = (LogService) logService;
	}
}
