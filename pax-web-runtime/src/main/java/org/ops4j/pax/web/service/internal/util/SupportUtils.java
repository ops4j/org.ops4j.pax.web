/*
 * Copyright 2013 Christoph Läubrich
 *
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
package org.ops4j.pax.web.service.internal.util;

/**
 * Allows to check if support for several optional modules is possible
 */
@SuppressWarnings("ConstantConditions")
public class SupportUtils {

	private SupportUtils() {
		// utils class
	}

	public static boolean isConfigurationAdminAvailable() {
		try {
			SupportUtils.class.getClassLoader().loadClass("org.osgi.service.cm.ConfigurationAdmin");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Check if {@link org.osgi.service.event.EventAdmin} is available
	 *
	 * @return <code>true</code> if EventAdmin class can be loaded,
	 * <code>false</code> otherwhise
	 */
	public static boolean isEventAdminAvailable() {
		try {
			return (org.osgi.service.event.EventAdmin.class != null);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	/**
	 * Check if {@link org.osgi.service.log.LogService} is available
	 *
	 * @return <code>true</code> if LogService class can be loaded,
	 * <code>false</code> otherwhise
	 */
	public static boolean isLogServiceAvailable() {
		try {
			return (org.osgi.service.log.LogService.class != null);
		} catch (NoClassDefFoundError ignore) {
			return false;
		}
	}

	/**
	 * Check if {@link org.osgi.service.log.LogService} is available
	 *
	 * @return <code>true</code> if LogService class can be loaded,
	 * <code>false</code> otherwhise
	 */
	public static boolean isLogServiceAvailable2() {
		try {
			SupportUtils.class.getClassLoader().loadClass(org.osgi.service.log.LogService.class.getName());
			return true;
		} catch (ClassNotFoundException ignore) {
			return false;
		}
	}

}
