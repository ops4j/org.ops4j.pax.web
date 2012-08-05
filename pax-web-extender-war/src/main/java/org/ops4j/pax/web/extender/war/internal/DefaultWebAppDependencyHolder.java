package org.ops4j.pax.web.extender.war.internal;

import javax.servlet.ServletContainerInitializer;

import org.ops4j.pax.web.service.WebAppDependencyHolder;
import org.osgi.service.http.HttpService;


public class DefaultWebAppDependencyHolder implements WebAppDependencyHolder {
	
	
	private HttpService httpService;

	public DefaultWebAppDependencyHolder(HttpService httpService) {
		this.httpService = httpService;
	}

	@Override
	public HttpService getHttpService() {
		return httpService;
	}

	@Override
	public ServletContainerInitializer getServletContainerInitializer() {
		return null;
	}

}
