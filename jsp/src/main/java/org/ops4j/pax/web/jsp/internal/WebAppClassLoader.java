/* Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.web.jsp.internal;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.ops4j.pax.swissbox.lang.BundleClassLoader;

/**
 * TODO
 *
 * @author Alin Dreghiciu
 * @since 0.3.0 January 08, 2008
 */
public class WebAppClassLoader
    extends BundleClassLoader
{

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog( WebAppClassLoader.class );

    public WebAppClassLoader( final Bundle bundle )
    {
        super( bundle );
    }

    @Override
    public URL[] getURLs()
    {
        final Bundle bundle = getBundle();
        final List<URL> urls = new ArrayList<URL>();
        final String bundleClasspath = (String) bundle.getHeaders().get( "Bundle-ClassPath" );
        if( bundleClasspath != null )
        {
            String[] segments = bundleClasspath.split( "," );
            for( String segment : segments )
            {
                final URL url = bundle.getEntry( segment );
                if( url != null )
                {
                    if( url.toExternalForm().endsWith( "jar" ) )
                    {
                        LOG.debug( "Usign url: " + url );
                        try
                        {
                            URL jarUrl = new URL( "jar:" + url.toExternalForm() + "!/" );
                            urls.add( jarUrl );
                            JarURLConnection conn = (JarURLConnection) jarUrl.openConnection();
                            conn.setUseCaches( false );
                            JarFile jarFile = null;
                            try
                            {
                                jarFile = conn.getJarFile();
                                Enumeration entries = jarFile.entries();
                                while( entries.hasMoreElements() )
                                {
                                    JarEntry entry = (JarEntry) entries.nextElement();
                                    String name = entry.getName();
                                    if( !name.startsWith( "META-INF/" ) )
                                    {
                                        continue;
                                    }
                                    if( !name.endsWith( ".tld" ) )
                                    {
                                        continue;
                                    }
                                    LOG.debug( name );
                                }
                            }
                            catch( Exception ex )
                            {
                                LOG.debug( ex.getMessage(), ex );
                                // if not in redeploy mode, close the jar in case of an error
                                if( jarFile != null )
                                {
                                    try
                                    {
                                        jarFile.close();
                                    }
                                    catch( Throwable t )
                                    {
                                        // ignore
                                    }
                                }
                            }
                            finally
                            {
                                // if in redeploy mode, always close the jar
                                if( jarFile != null )
                                {
                                    try
                                    {
                                        jarFile.close();
                                    } catch( Throwable t )
                                    {
                                        // ignore
                                    }
                                }
                            }
                        }
                        catch( MalformedURLException ignore )
                        {
                            LOG.debug( ignore.getMessage() );
                        }
                        catch( IOException ignore )
                        {
                            LOG.debug( ignore.getMessage() );
                        }
                    }
                }
            }
        }

        LOG.debug( "URLs: " + urls );
        try
        {
            throw new Exception();
        }
        catch( Exception e )
        {
            LOG.debug("stacktrace",  e);
        }
        return urls.toArray( new URL[urls.size()] );
    }
}
