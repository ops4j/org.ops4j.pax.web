package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

/**
 * @author Romaim Gilles
 */
public class ConfigFileParsingException extends RuntimeException {

	/**
     * 
     */
	private static final long serialVersionUID = 973850424721229853L;

	public ConfigFileParsingException(File configFile, Throwable cause) {
		super(String.format("cannot read the configuration file: %s",
				configFile.getAbsolutePath()), cause);
	}
}
