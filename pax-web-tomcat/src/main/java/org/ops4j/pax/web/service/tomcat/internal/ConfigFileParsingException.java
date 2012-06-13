package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/11/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigFileParsingException extends RuntimeException {

    public ConfigFileParsingException(File configFile, Throwable cause) {
        super(String.format("cannot read the configuration file: %s", configFile.getAbsolutePath()), cause);
    }
}
