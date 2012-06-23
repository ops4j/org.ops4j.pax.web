/*
 * Copyright 2012 Romain Gilles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.ops4j.pax.web.service.tomcat.internal;

import java.io.*;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.spi.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Romain Gilles
 */
public class TomcatServerFactory implements ServerFactory
{
    private static final Logger LOG = LoggerFactory.getLogger( TomcatServerFactory.class );

    public TomcatServerFactory()
    {
    }

    @Override
    public ServerWrapper newServer(Configuration configuration)
    {
        return TomcatServerWrapper.getInstance( EmbeddedTomcat.newEmbeddedTomcat( configuration ) );
    }
}

class EmbeddedTomcat extends Tomcat
{
    private static final Logger LOG = LoggerFactory.getLogger( EmbeddedTomcat.class );

    public static final String SERVER_CONFIG_FILE_NAME = "tomcat-server.xml";

    private EmbeddedTomcat()
    {
    }

    static EmbeddedTomcat newEmbeddedTomcat(Configuration configuration)
    {
        EmbeddedTomcat result = new EmbeddedTomcat();
        result.configure( configuration );
        return result;
    }

    private static class FakeCatalina extends Catalina
    {
        @Override
        protected Digester createStartDigester()
        {
            return super.createStartDigester();
        }
    }

    void configure(Configuration configuration)
    {
        long start = System.nanoTime();
        initBaseDir( configuration );
        Digester digester = new FakeCatalina().createStartDigester();
        digester.push( this );
//        digester.setClassLoader(getClass().getClassLoader()); //TODO see if we need to work on class loader
        File configurationFile = new File( configuration.getConfigurationDir(), SERVER_CONFIG_FILE_NAME );
        InputStream configurationStream = null;
        try
        {
            configurationStream = new FileInputStream( configurationFile );
            digester.parse( configurationStream );
            long elapsed = start - System.nanoTime();
            if( LOG.isInfoEnabled() )
            {
                LOG.info( "configuration processed in {} ms", ( elapsed / 1000000 ) );
            }
        } catch( FileNotFoundException e )
        {
            throw new ConfigFileNotFoundException( configurationFile, e );
        } catch( IOException e )
        {
            throw new ConfigFileParsingException( configurationFile, e );
        } catch( SAXException e )
        {
            throw new ConfigFileParsingException( configurationFile, e );
        } finally
        {
            //TODO close the file org.eclipse.virgo.util.io.IOUtils
            if( configurationStream != null )
            {
                try
                {
                    configurationStream.close();
                } catch( IOException e )
                {
                    LOG.debug( "cannot close the configuration file '{}' properly", configurationFile, e );
                }
            }
        }
        //TODO For the moment we do nothing with the defaults context.xml, web.xml. They are used when you want to deploy web app
    }

    private void initBaseDir(Configuration configuration)
    {
        setBaseDir( configuration.getTemporaryDirectory().getAbsolutePath() ); //TODO do we put the canonical insteadof?
//        super.initBaseDir(); //TODO do it if it is required
    }

    String getBasedir()
    {
        return basedir;
    }

    Context findContext(String contextName)
    {
        return (Context) findContainer( contextName );
    }

    Container findContainer(String contextName)
    {
        return getHost().findChild( contextName );
    }
}