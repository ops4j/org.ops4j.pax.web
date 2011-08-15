/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 *
 */
public class AnnotationScanner<T> {

	protected final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	protected String className;
	
	private Class clazz;
	

	public AnnotationScanner(String className) {
		this.className = className;
	}
	
	public Class loadClass() {
		
		if (clazz == null) {
			try {
				ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
				clazz = contextClassLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				LOG.warn("Given class of type %s identified by %s annotation can't be created", className, this.getClass().getName());
			}
		}		
		
		return clazz;
	}

	protected boolean initParamsContain(WebAppInitParam[] initParams, String name) {
		for (WebAppInitParam webAppInitParam : initParams) {
			if (webAppInitParam.getParamName().equalsIgnoreCase(name))
				return true;
		}
	
		return false;
	}
	
}
