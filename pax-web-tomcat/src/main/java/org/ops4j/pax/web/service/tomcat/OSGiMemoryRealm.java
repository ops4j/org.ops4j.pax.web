package org.ops4j.pax.web.service.tomcat;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.realm.MemoryRealm;
import org.apache.tomcat.util.digester.Digester;

public class OSGiMemoryRealm extends MemoryRealm {

	@Override
	protected void startInternal() throws LifecycleException {

		if (getPathname().startsWith("classpath")) {

			String pathName = getPathname();
			try {
				URL pathUrl = new URL(pathName);
				pathName = pathUrl.getHost();
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			ClassLoader classLoader = getClass().getClassLoader();
			InputStream inputStream = classLoader
					.getResourceAsStream(pathName);

			if (inputStream == null) {
				Enumeration<URL> resources;
				try {
					resources = classLoader.getResources(
							pathName);
					while (resources.hasMoreElements()) {
						URL nextElement = resources.nextElement();
						inputStream = nextElement.openStream();
						continue;
					}
					
					
				} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
			}

			Digester digester = getDigester();
			try {
				synchronized (digester) {
					digester.push(this);
					digester.parse(inputStream);
				}
			} catch (Exception e) {
				throw new LifecycleException(
						sm.getString("memoryRealm.readXml"), e);
			} finally {
				digester.reset();
			}

			// Create a MessageDigest instance for credentials, if desired
			if (digest != null) {
				try {
					md = MessageDigest.getInstance(digest);
				} catch (NoSuchAlgorithmException e) {
					throw new LifecycleException(sm.getString(
							"realmBase.algorithm", digest), e);
				}
			}

			setState(LifecycleState.STARTING);
		} else {
			super.startInternal();
		}

	}

}
