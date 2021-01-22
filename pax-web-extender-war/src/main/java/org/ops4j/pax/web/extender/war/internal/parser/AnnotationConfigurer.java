/*
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
package org.ops4j.pax.web.extender.war.internal.parser;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author achim
 */
public class AnnotationConfigurer<T> {

	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	protected String className;

	private Class<?> clazz;

	private Bundle bundle;

	public AnnotationConfigurer(Bundle bundle, String className) {
		this.bundle = bundle;
		this.className = className;
	}

	public Class<?> loadClass() {

		if (clazz == null) {
			try {
				clazz = bundle.loadClass(className);
			} catch (ClassNotFoundException e) {
				log.warn(
						"Given class of type {} identified by {} annotation can't be created",
						className, this.getClass().getName());
			}
		}

		return clazz;
	}

//	protected boolean initParamsContain(WebAppInitParam[] initParams,
//										String name) {
//		for (WebAppInitParam webAppInitParam : initParams) {
//			if (webAppInitParam.getParamName().equalsIgnoreCase(name)) {
//				return true;
//			}
//		}
//
//		return false;
//	}

}
