package org.ops4j.pax.web.extender.war.internal;

import java.util.List;

import org.ops4j.lang.NullArgumentException;
import org.ops4j.pax.web.extender.war.internal.model.WebApp;
import org.ops4j.pax.web.extender.war.internal.parser.dom.DOMWebXmlParser;
import org.ops4j.pax.web.extender.war.internal.util.ManifestUtil;
import org.ops4j.pax.web.service.spi.WebEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ServletObserver extends WebObserver<String> {

	private WebXmlParser parser;

	public ServletObserver(final WebXmlParser parser, WebAppPublisher publisher,
			WebEventDispatcher eventDispatcher,
			DefaultWebAppDependencyManager dependencyManager,
			BundleContext bundleContext) {
		super(publisher, eventDispatcher, dependencyManager, bundleContext);
		this.parser = parser;
	}

	@Override
	public void addingEntries(Bundle bundle, List<String> entries) {
		NullArgumentException.validateNotNull( bundle, "Bundle" );
        NullArgumentException.validateNotNull( entries, "List of Servlets" );

        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        //Context name is also needed for some of the pre-condition checks, therefore it is retrieved here.
        String contextName = ManifestUtil.extractContextName(bundle);
        LOG.info( String.format( "Using [%s] as web application context name", contextName ) );
        
        List<String> virtualHostList = extractVirtualHostList(bundle);
        LOG.info( String.format( "[%d] virtual hosts defined in bundle header", virtualHostList.size()));
        
        List<String> connectorList = extractConnectorList(bundle);
        LOG.info( String.format( "[%d] connectors defined in bundle header", connectorList.size()));
        
        if( webApps.containsKey(bundle.getBundleId()) ) {
            LOG.debug(String.format("Already found a web application in bundle %d", bundle.getBundleId()));
            return;
        }
        
        String rootPath = extractRootPath(bundle);
        LOG.info( String.format( "Using [%s] as web application root path", rootPath ) );
        
        try {
	        WebApp webApp = ((DOMWebXmlParser)parser).parseAnnotatedServlets(bundle);
	        
	        webApp.setVirtualHostList(virtualHostList);
	        webApp.setConnectorList(connectorList);
	        webApp.setBundle( bundle );
	        webApp.setContextName( contextName );
	        webApp.setRootPath( rootPath );
	        webApp.setDeploymentState(WebApp.UNDEPLOYED_STATE);
	
	        webApps.put(bundle.getBundleId(), webApp);
	        
	        if (ManifestUtil.getHeader(bundle, "Pax-ManagedBeans") == null) {
	        	dependencyManager.addWebApp(webApp);
	        }
	
	        // The Webapp-Deploy header controls if the app is deployed on
	        // startup.
	        if( "true".equals(opt(ManifestUtil.getHeader(bundle, "Webapp-Deploy"), "true"))) {
	            deploy(webApp);
	        } else {
	            eventDispatcher.webEvent(new WebEvent(WebEvent.UNDEPLOYED, "/" + webApp.getContextName(), webApp.getBundle(), bundleContext.getBundle()));
	        }
        } catch (Exception ignore) {
        	LOG.error( "Errer while interpreting Annotated Servlets", ignore );
            eventDispatcher.webEvent(new WebEvent(WebEvent.FAILED, "/"+contextName, bundle, bundleContext.getBundle(), ignore));
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
	}

	@Override
	public void removingEntries(Bundle bundle, List<String> entries) {
		// TODO Auto-generated method stub
		
	}

}
