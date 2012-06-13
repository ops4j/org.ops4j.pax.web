package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/11/12
 * Time: 1:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigFileNotFoundException extends RuntimeException {
    public ConfigFileNotFoundException(File configFile, Throwable cause) {
        super(String.format("cannot parse the configuration file: %s", configFile.getAbsolutePath()), cause);
    }
}
