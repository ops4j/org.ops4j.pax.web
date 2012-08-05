package org.ops4j.pax.web.service;

import javax.servlet.ServletContainerInitializer;

import org.osgi.service.http.HttpService;


public interface WebAppDependencyHolder {
	
	HttpService getHttpService();
	ServletContainerInitializer getServletContainerInitializer();
	

}
