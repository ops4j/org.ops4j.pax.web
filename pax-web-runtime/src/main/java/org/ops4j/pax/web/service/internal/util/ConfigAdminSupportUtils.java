package org.ops4j.pax.web.service.internal.util;

import org.osgi.service.cm.ManagedService;

public class ConfigAdminSupportUtils {
	
	private ConfigAdminSupportUtils() {
		//utils class
	}
	
	public static boolean configAdminSupportAvailable() {
		try {
			return (ManagedService.class != null);
		} catch ( NoClassDefFoundError ignore ) {
			return false;
		}
	}

}
