package org.ops4j.pax.web.deployer.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;

/**
 * An Apache Felix FileInstall transform for WAR files.
 *
 * @author Alin Dreghiciu
 */
public class WarDeployer
    implements ArtifactUrlTransformer
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WarDeployer.class );

    public boolean canHandle( final File artifact )
    {
        // TODO maybe better would be to open the file and look for web.xml
        if( !artifact.isFile() || !artifact.getName().endsWith( "war" ) )
        {
            return false;
        }

        try
        {
            new URL( "webbundle", null, artifact.toURL().toExternalForm() );
        }
        catch( MalformedURLException e )
        {
            LOG.warn(
                String.format(
                    "File %s could nto be transformed. Most probably that Pax URL WAR handler is not installed",
                    artifact.getAbsolutePath()
                )
            );
            return false;
        }

        return true;
    }

    public URL transform( final URL artifact )
        throws Exception
    {
        final String path = artifact.getPath();
        if( path != null )
        {
            final int idx = path.lastIndexOf( "/" );
            if( idx > 0 )
            {
                final String[] name = path.substring( idx + 1 ).split( "\\." );
                final StringBuilder url = new StringBuilder();
                url.append( artifact.toExternalForm() );
                if( artifact.toExternalForm().contains( "?" ) )
                {
                    url.append( "&" );
                }
                else
                {
                    url.append( "?" );
                }
                url.append( "Webapp-Context=" ).append( name[0] );
                url.append( "&" );
                url.append( "Bundle-SymbolicName=" ).append( name[0] );

                return new URL( "webbundle", null, url.toString() );
            }
        }
        return new URL( "webbundle", null, artifact.toExternalForm() );
    }

}
