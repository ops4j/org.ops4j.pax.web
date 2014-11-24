package org.ops4j.pax.web.utils;

import javax.servlet.DispatcherType;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterAnnotationScanner {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public Boolean scanned = false;
	public String[] urlPatterns;
	public String filterName;
	public Integer loadOnStartup;
	public Boolean asyncSupported;
	public WebInitParam[] webInitParams;
	public MultipartConfig multiPartConfigAnnotation;
	public WebFilter filterAnnotation;
	public String[] servletNames;

	public DispatcherType[] dispatcherTypes;

	public FilterAnnotationScanner(Class<?> clazz) {
		filterAnnotation = (WebFilter) clazz.getAnnotation(WebFilter.class);

		if (filterAnnotation == null)
			return;

		scanned = true;

		multiPartConfigAnnotation = (MultipartConfig) clazz.getAnnotation(MultipartConfig.class);
	
		if (filterAnnotation.urlPatterns().length > 0
				&& filterAnnotation.value().length > 0) {
			log.warn(clazz.getName()
					+ " defines both @WebFilter.value and @WebFilter.urlPatterns");
			return;
		}
	
		urlPatterns = filterAnnotation.value();
		if (urlPatterns.length == 0) {
			urlPatterns = filterAnnotation.urlPatterns();
		}
	
		filterName = (filterAnnotation.filterName().equals("") ? clazz
				.getName()
				: filterAnnotation.filterName());

	
		webInitParams = filterAnnotation.initParams();

		servletNames = filterAnnotation.servletNames();

		dispatcherTypes = filterAnnotation.dispatcherTypes();

		asyncSupported = filterAnnotation.asyncSupported();

	}

}
