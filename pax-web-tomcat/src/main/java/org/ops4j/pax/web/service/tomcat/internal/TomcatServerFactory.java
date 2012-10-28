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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.ServletContainerInitializer;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.apache.tomcat.util.digester.Digester;
import org.ops4j.pax.web.service.spi.Configuration;
import org.osgi.service.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Romain Gilles
 */
public class TomcatServerFactory implements ServerFactory {
	private static final Logger LOG = LoggerFactory
			.getLogger(TomcatServerFactory.class);

	public TomcatServerFactory() {
	}

	@Override
	public ServerWrapper newServer(Configuration configuration) {
		return TomcatServerWrapper.getInstance(EmbeddedTomcat
				.newEmbeddedTomcat(configuration));
	}
}

class EmbeddedTomcat extends Tomcat {
	private static final Logger LOG = LoggerFactory
			.getLogger(EmbeddedTomcat.class);

	public static final String SERVER_CONFIG_FILE_NAME = "tomcat-server.xml";

	private EmbeddedTomcat() {
	}

	static EmbeddedTomcat newEmbeddedTomcat(Configuration configuration) {
		EmbeddedTomcat result = new EmbeddedTomcat();
		result.configure(configuration);
		return result;
	}

	private static class FakeCatalina extends Catalina {
		@Override
		protected Digester createStartDigester() {
			return super.createStartDigester();
		}
	}

	//TODO: still needs to take the service configuration through config-admin-service
	//TODO: merge configuration wich might come from config-admin-service and a server.xml 
	void configure(Configuration configuration) {
		long start = System.nanoTime();
		initBaseDir(configuration);
		Digester digester = new FakeCatalina().createStartDigester();
		// digester.setClassLoader(classLoader); //TODO see if we need to work
		// on class loader
		digester.push(this);
		File configurationFile = new File(configuration.getConfigurationDir(),
				SERVER_CONFIG_FILE_NAME);
		if (configurationFile.exists()) {
			InputStream configurationStream = null;
			try {
				configurationStream = new FileInputStream(configurationFile);
				digester.parse(configurationStream);
				long elapsed = start - System.nanoTime();
				if (LOG.isInfoEnabled()) {
					LOG.info("configuration processed in {} ms",
							(elapsed / 1000000));
				}
			} catch (FileNotFoundException e) {
				throw new ConfigFileNotFoundException(configurationFile, e);
			} catch (IOException e) {
				throw new ConfigFileParsingException(configurationFile, e);
			} catch (SAXException e) {
				throw new ConfigFileParsingException(configurationFile, e);
			} finally {
				// TODO close the file org.eclipse.virgo.util.io.IOUtils
				if (configurationStream != null) {
					try {
						configurationStream.close();
					} catch (IOException e) {
						LOG.debug(
								"cannot close the configuration file '{}' properly",
								configurationFile, e);
					}
				}
			}
		} else {
			this.setPort(configuration.getHttpPort());
			this.setHostname(configuration.getHttpConnectorName());
		}
		// TODO For the moment we do nothing with the defaults context.xml,
		// web.xml. They are used when you want to deploy web app
	}

	private void initBaseDir(Configuration configuration) {
		setBaseDir(configuration.getTemporaryDirectory().getAbsolutePath()); 
		// TODO do we put the canonical insteadof?
		// super.initBaseDir(); 
		// TODO do it if it is required
	}

	String getBasedir() {
		return basedir;
	}

	Context findContext(String contextName) {
		return (Context) findContainer(contextName);
	}

	Container findContainer(String contextName) {
		return getHost().findChild(contextName);
	}
	
	public Context addContext(
			Map<String, String> contextParams,
			Map<String, Object> contextAttributes,
			String contextName,
			HttpContext httpContext,
			AccessControlContext accessControllerContext,
			Map<ServletContainerInitializer, Set<Class<?>>> containerInitializers,
			URL jettyWebXmlURL, List<String> virtualHosts,
			List<String> connectors, String basedir) {
        silence(host, "/"+contextName);
        Context ctx = new HttpServiceContext();
        ctx.setName(contextName);
        ctx.setPath("/"+contextName);
        ctx.setDocBase(basedir);
        ctx.addLifecycleListener(new FixContextListener());
        //new OSGi methods
        ((HttpServiceContext) ctx).setHttpContext(httpContext);
        //TODO: what about the AccessControlContext?
        //TODO: the virtual host section below 
        //TODO: what about the VirtualHosts?
        //TODO: what about the tomcat-web.xml config?
        //TODO: connectors are needed for virtual host?
        if (containerInitializers != null) {
	        for (Entry<ServletContainerInitializer, Set<Class<?>>> entry : containerInitializers.entrySet()) {
				ctx.addServletContainerInitializer(entry.getKey(), entry.getValue());
			}
        }
        if (host == null) {
            getHost().addChild(ctx);
        } else {
            host.addChild(ctx);
        }
        return ctx;
    }
	
	private void silence(Host host, String ctx) {
        String base = "org.apache.catalina.core.ContainerBase.[default].[";
        if (host == null) {
            base += getHost().getName();
        } else {
            base += host.getName();
        }
        base += "].[";
        base += ctx; 
        base += "]";
        LOG.warn(base);
    }

}