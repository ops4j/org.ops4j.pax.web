package org.ops4j.pax.web.service.tomcat.internal;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.startup.*;
import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: romain.gilles
 * Date: 6/9/12
 * Time: 7:50 AM
 * To change this template use File | Settings | File Templates.
 */
public class TomcatServerFactory implements ServerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TomcatServerFactory.class);
    private final File configFile;

    public TomcatServerFactory(File configFile) {
        this.configFile = configFile;
    }

    public Server getServer() {
        return server;
    }

    public void setServer(Server server) {
        this.server = server;
    }

    /**
     * The server component we are starting or stopping.
     */
    private Server server = null;

    @Override
    public ServerWrapper newServer() {
        long t1 = System.nanoTime();
        //initNaming()
        // Create and execute our Digester
        //catalina-deployer, tomcat-embed-core, tomcat-coyote
        Digester digester = createStartDigester();
        initSystemProperties();
        InputSource inputSource = null;
        InputStream inputStream = null;
        File file = getConfigFile();

        try {

            inputStream = new FileInputStream(file);
            inputSource = new InputSource(file.getAbsoluteFile().toURI().toString());
        } catch (FileNotFoundException e) {
            throw new ConfigFileNotFoundException(file, e);
        }

        try {
            inputSource.setByteStream(inputStream);
            digester.push(this);
            digester.parse(inputSource);
        } catch (SAXParseException spe) {
            throw new ConfigFileParsingException(file, spe);
        } catch (SAXException e) {
            throw new ConfigFileParsingException(file, e);
        } catch (IOException e) {
            throw new ConfigFileParsingException(file, e);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        // Start the new server
        try {
            getServer().init();
        } catch (LifecycleException e) {

        }

        long t2 = System.nanoTime();
        if(LOG.isInfoEnabled()) {
            LOG.info("Initialization processed in " + ((t2 - t1) / 1000000) + " ms");
        }

        return TomcatServerWrapper.getInstance(getServer());
    }

    private void initSystemProperties() {

    }

    private static class FakeCatalina extends Catalina {
        @Override
        protected Digester createStartDigester() {
            return super.createStartDigester();
        }
    }
    /**
     * Create and configure the Digester we will be using for startup.
     */
    protected Digester createStartDigester() {
        return (new FakeCatalina()).createStartDigester();
    }

    public File getConfigFile() {
        return configFile;
    }
}

class EmbeddedTomcat extends Tomcat {

}