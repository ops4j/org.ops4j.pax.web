package org.ops4j.pax.web.extender.war.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.service.spi.WarManager;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebObserver<E> implements BundleObserver<E>, WarManager {
	
	/**
     * Logger.
     */
    static final Logger LOG = LoggerFactory.getLogger( WebObserver.class );

	/**
	 * Web app publisher.
	 */
	protected final WebAppPublisher publisher;
	/**
	 * Mapping between the bundle id and WebApp
	 */
	protected final HashMap<Long, WebApp> webApps = new HashMap<Long, WebApp>();
	/**
	 * Mapping between the bundle id and WebApp
	 */
	private final HashMap<Long, WebApp> defaultWebApps = new HashMap<Long, WebApp>();
	/**
	 * The queue of published WebApp objects to a context.
	 */
	private final HashMap<String, LinkedList<WebApp>> contexts = new HashMap<String, LinkedList<WebApp>>();
	protected final BundleContext bundleContext;
	protected final WebEventDispatcher eventDispatcher;
	protected DefaultWebAppDependencyManager dependencyManager;

	private static String stripPrefix(String value, String prefix) {
	    if( value.startsWith( prefix ) )
	    {
	    	return value.substring( prefix.length() );
	    }
	    return value;
	}

	protected void deploy(WebApp webApp) {
	
	    Bundle bundle = webApp.getBundle();
	    String contextName = webApp.getContextName();
	
	    eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYING, "/" + contextName, bundle, bundleContext.getBundle()));
	    if ( !contexts.containsKey(contextName) ) {
	        LinkedList<WebApp> queue = new LinkedList<WebApp>();
	        contexts.put(contextName, queue);
	        queue.add(webApp);
	
	        // let the publisher set the deployment state and send web event
	        publisher.publish(webApp, eventDispatcher, bundleContext);
	    } else {
	        LinkedList<WebApp> queue = contexts.get(contextName);
	        queue.add(webApp);
	
	        Collection<Long> duplicateIds = new LinkedList<Long>();
	        for (WebApp duplicateWebApp : queue) {
	            duplicateIds.add(duplicateWebApp.getBundle().getBundleId());
	        }
	
	        webApp.setDeploymentState(WebApp.WAITING_STATE);
	        eventDispatcher.webEvent(new WebEvent(WebEvent.WAITING, "/"+contextName, bundle, bundleContext.getBundle(), duplicateIds ));
	    }
	
	}

	protected void undeploy(WebApp webApp) {
	
	    String contextName = webApp.getContextName();
	    LinkedList<WebApp> queue = contexts.get(contextName);
	    if( queue!=null ) {
	
	        // Are we the published web app??
	        if( queue.get(0) == webApp ) {
	
	            webApp.setDeploymentState(WebApp.UNDEPLOYED_STATE);
	            eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYING, "/"+ contextName, webApp.getBundle(), bundleContext.getBundle()));
	            publisher.unpublish( webApp );
	            eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED, "/"+ contextName, webApp.getBundle(), bundleContext.getBundle()));
	            queue.removeFirst();
	
	            //Below checks if another webapp is waiting for the context, if so the webapp is published.
	            LOG.debug("Check for a waiting webapp.");
	            if( !queue.isEmpty() ) {
	                LOG.debug("Found another bundle waiting for the context");
	                WebApp next = queue.getFirst();
	
	                eventDispatcher.webEvent(new WebEvent(WebEvent.DEPLOYING, "/"+contextName, next.getBundle(), bundleContext.getBundle()));
	
	                // let the publisher set the deployment state and send web event
	                publisher.publish(next, eventDispatcher, bundleContext);
	            } else {
	                contexts.remove(contextName);
	            }
	
	        } else if( queue.remove(webApp) ) {
	            webApp.setDeploymentState(WebApp.UNDEPLOYED_STATE);
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
	    if( webApp.getDeploymentState()!=WebApp.UNDEPLOYED_STATE ) {
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
	    if( webApp.getDeploymentState()==WebApp.UNDEPLOYED_STATE ) {
	        return ALREADY_STOPPED;
	    }
	    undeploy(webApp);
	    return SUCCESS;
	}

	/**
	 * @param bundle
	 * @return
	 */
	protected String extractRootPath(final Bundle bundle) {
	    String rootPath = ManifestUtil.getHeader(bundle, "Webapp-Root");
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

	protected static <T> T opt(T value, T orElse) {
	    if( value== null ) {
	        return orElse;
	    }
	    return value;
	}

	private static String stripSuffix(String value, String suffix) {
	    if( value.endsWith( suffix ) )
	    {
	    	return value.substring(0,  value.length() - suffix.length() );
	    }
	    return value;
	}

	public WebObserver(WebAppPublisher publisher,
			WebEventDispatcher eventDispatcher,
			DefaultWebAppDependencyManager dependencyManager,
			BundleContext bundleContext) {
		
		NullArgumentException.validateNotNull( publisher, "Web App Publisher" );
        NullArgumentException.validateNotNull( eventDispatcher, "WebEvent Dispatcher" );
        NullArgumentException.validateNotNull( dependencyManager, "DefaultWebAppDependencyManager" );
        NullArgumentException.validateNotNull( bundleContext, "BundleContext" );

		this.publisher = publisher;
        this.bundleContext = bundleContext;
        this.dependencyManager = dependencyManager;
        this.eventDispatcher = eventDispatcher;
	}

	public Collection<Long> getDefaultWebBundles() {
		return defaultWebApps.keySet();
	}

	protected List<String> extractVirtualHostList(final Bundle bundle) {
		List<String> virtualHostList = new LinkedList<String>();
		String virtualHostListAsString = ManifestUtil.getHeader(bundle,"Web-VirtualHosts");
		if ((virtualHostListAsString != null) && (virtualHostListAsString.length() > 0)){
			String[] virtualHostArray = virtualHostListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				virtualHostList.add(virtualHost.trim());
			}
		}
		return virtualHostList;
	}

	protected List<String> extractConnectorList(final Bundle bundle) {
		List<String> connectorList = new LinkedList<String>();
		String connectorListAsString = ManifestUtil.getHeader(bundle,"Web-Connectors");
		if ((connectorListAsString != null) && (connectorListAsString.length() > 0)){
			String[] virtualHostArray = connectorListAsString.split(",");
			for (String virtualHost : virtualHostArray) {
				connectorList.add(virtualHost.trim());
			}
		}
		return connectorList;
	}

}