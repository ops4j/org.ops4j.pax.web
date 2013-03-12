package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

/**
 * @author Romaim Gilles
 */
public class ConfigFileNotFoundException extends RuntimeException {
	/**
     * 
     */
	private static final long serialVersionUID = -5213267690789184307L;

	public ConfigFileNotFoundException(File configFile, Throwable cause) {
		super(String.format("cannot parse the configuration file: %s",
				configFile.getAbsolutePath()), cause);
	}
}
