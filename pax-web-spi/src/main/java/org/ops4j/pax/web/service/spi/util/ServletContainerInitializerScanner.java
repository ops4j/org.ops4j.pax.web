/*
 * Copyright 2016 Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ops4j.pax.web.service.spi.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.annotation.HandlesTypes;

import org.apache.xbean.finder.BundleAnnotationFinder;
import org.apache.xbean.finder.BundleAssignableClassFinder;
import org.ops4j.pax.web.utils.ClassPathUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletContainerInitializerScanner {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private Bundle bundle;
	private Bundle serverBundle;
	private PackageAdmin packageAdminService;


	public ServletContainerInitializerScanner(Bundle bundle, Bundle serverBundle, PackageAdmin packageAdminService) {
		this.bundle = bundle;
		this.serverBundle = serverBundle;
		this.packageAdminService = packageAdminService;
	}


	public void scanBundles(Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers) {
		// scan for ServletContainerInitializers
		Set<Bundle> bundlesInClassSpace = ClassPathUtil.getBundlesInClassSpace(bundle, new HashSet<>());

		if (serverBundle != null) {
			ClassPathUtil.getBundlesInClassSpace(serverBundle, bundlesInClassSpace);
		}

		for (URL u : ClassPathUtil.listResources(bundlesInClassSpace, "/META-INF/services",
				"javax.servlet.ServletContainerInitializer", true)) {
			try {
				InputStream is = u.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is));
				// only the first non-empty and non-comment line is read, it contains
				// the name of the class.
				String className = parseServiceConfig(reader);
				log.info("will add {} to ServletContainerInitializers", className);

				if (className.endsWith("JasperInitializer")) {
					log.info("Skipt {}, because specialized handler will be present", className);
					continue;
				}

				Class<?> initializerClass;

				try {
					initializerClass = bundle.loadClass(className);
				} catch (ClassNotFoundException ignore) {
					if (serverBundle != null) {
						initializerClass = serverBundle.loadClass(className);
					} else {
						log.warn("couldn't find Class for {}", className);
						continue;
					}
				}

				ServletContainerInitializer initializer = (ServletContainerInitializer) initializerClass.newInstance();

				Set<Class<?>> setOfClasses = new HashSet<>();
				// scan for @HandlesTypes
				HandlesTypes handlesTypes = initializerClass.getAnnotation(HandlesTypes.class);
				if (handlesTypes != null) {
					Class<?>[] classes = handlesTypes.value();

					for (Class<?> klass : classes) {
						boolean isAnnotation = klass.isAnnotation();
						boolean isInteraface = klass.isInterface();

						if (isAnnotation) {
							try {
								BundleAnnotationFinder baf = new BundleAnnotationFinder(
										packageAdminService, bundle);
								@SuppressWarnings("unchecked")
								List<Class<?>> annotatedClasses = baf
										.findAnnotatedClasses((Class<? extends Annotation>) klass);
								setOfClasses.addAll(annotatedClasses);
							} catch (Exception e) {
								log.warn("Failed to find annotated classes for ServletContainerInitializer");
							}
						} else if (isInteraface) {
							BundleAssignableClassFinder basf = new BundleAssignableClassFinder(
									packageAdminService, new Class[]{klass}, bundle);
							Set<String> interfaces = basf.find();
							for (String interfaceName : interfaces) {
								setOfClasses.add(bundle.loadClass(interfaceName));
							}
						} else {
							// class
							BundleAssignableClassFinder basf = new BundleAssignableClassFinder(
									packageAdminService, new Class[]{klass}, bundle);
							Set<String> classNames = basf.find();
							for (String klassName : classNames) {
								setOfClasses.add(bundle.loadClass(klassName));
							}
						}
					}
				}
				containerInitializers.put(initializer, setOfClasses);
				log.info("added ServletContainerInitializer: {}", className);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
				log.warn("failed to parse and instantiate of javax.servlet.ServletContainerInitializer in classpath");
			}
		}
	}

	private String parseServiceConfig(BufferedReader r) throws IOException  {
		String ln;
		do {
			ln = r.readLine();
			if (ln == null) {
				// no more lines, abort
				return null;
			}
			// remove comments
			int ci = ln.indexOf('#');
			if (ci >= 0) {
				ln = ln.substring(0, ci);
			}
			ln = ln.trim();
			// if the line is empty read the next one
		} while (ln.isEmpty());
		return ln;
	}

}
