package org.ops4j.pax.web.service.tomcat.internal;

import java.io.File;

/**
 * @author Romaim Gilles
 */
public class ConfigFileNotFoundException extends RuntimeException
{
    public ConfigFileNotFoundException(File configFile, Throwable cause)
    {
        super( String.format( "cannot parse the configuration file: %s", configFile.getAbsolutePath() ), cause );
    }
}
