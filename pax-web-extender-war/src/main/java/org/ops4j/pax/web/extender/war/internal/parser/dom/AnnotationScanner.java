/**
 * 
 */
package org.ops4j.pax.web.extender.war.internal.parser.dom;

import org.ops4j.pax.web.extender.war.internal.model.WebAppInitParam;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 * 
 */
public class AnnotationScanner<T> {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected String className;

	private Class<?> clazz;

	private Bundle bundle;

	public AnnotationScanner(Bundle bundle, String className) {
		this.bundle = bundle;
		this.className = className;
	}

	public Class<?> loadClass() {

		if (clazz == null) {
			try {
				clazz = bundle.loadClass(className);
				// ClassLoader contextClassLoader =
				// Thread.currentThread().getContextClassLoader();
				// clazz = contextClassLoader.loadClass(className);
			} catch (ClassNotFoundException e) {
				log.warn(
						"Given class of type {} identified by {} annotation can't be created",
						className, this.getClass().getName());
			}
		}

		return clazz;
	}

	protected boolean initParamsContain(WebAppInitParam[] initParams,
			String name) {
		for (WebAppInitParam webAppInitParam : initParams) {
			if (webAppInitParam.getParamName().equalsIgnoreCase(name)) {
				return true;
			}
		}

		return false;
	}

}
