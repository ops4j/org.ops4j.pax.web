/*
 * Copyright 2020 OPS4J.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.web.jsp;

import java.util.Set;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.jasper.servlet.TldScanner;
import org.apache.tomcat.SimpleInstanceManager;

/**
 * <p>Pax Web extends original initializer, so it is possible to override the {@link TldScanner}.</p>
 *
 * <p>This initializer is <strong>not</strong> declared in {@code /META-INF/services/javax.servlet.ServletContainerInitializer}
 * and if needed, it is used directly by Pax Web to configure the context(s) when JSP support is required.</p>
 *
 * <p>According to Servlet specification 8.3 "JSP container pluggability", JSP processing/parsing/setup is no longer
 * performed by "Servlet container" itself and instead can be delegated to "JSP container" using
 * {@link ServletContainerInitializer} mechanism.</p>
 */
public class JasperInitializer extends org.apache.jasper.servlet.JasperInitializer {

	@Override
	public void onStartup(Set<Class<?>> types, ServletContext context) throws ServletException {
		// override the instance manager required by JasperServlet
		context.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());

		// call to perform TLD scanning
		super.onStartup(types, context);
	}

	@Override
	protected TldScanner newTldScanner(ServletContext context, boolean namespaceAware, boolean validate, boolean blockExternal) {
		return super.newTldScanner(context, namespaceAware, validate, blockExternal);
	}

}
