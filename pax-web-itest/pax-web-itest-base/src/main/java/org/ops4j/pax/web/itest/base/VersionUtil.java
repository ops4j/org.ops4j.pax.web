/**
 * 
 */
package org.ops4j.pax.web.itest.base;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author anierbeck
 *
 */
public class VersionUtil {

	private static final String PROJECT_VERSION;
	private static final String MY_FACES_VERSION;
	
	static {
		String projectVersion = "";
		String myFacesVersion = "";
		
		projectVersion = System.getProperty("ProjectVersion");
		myFacesVersion = System.getProperty("MyFacesVersion");
		
		try {
            final InputStream is = VersionUtil.class.getClassLoader().getResourceAsStream(
                "META-INF/pax-web-version.properties");
            if (is != null) {
                final Properties properties = new Properties();
                properties.load(is);
                projectVersion = properties.getProperty("pax.web.version", "").trim();
                myFacesVersion = properties.getProperty("myfaces.version", "").trim();
            }
        }
        catch (IOException ignore) {
            // use default versions
        }
		
		PROJECT_VERSION = projectVersion;
		MY_FACES_VERSION = myFacesVersion;
	}
	
	public static String getProjectVersion() {
		return PROJECT_VERSION;
	}

	public static String getMyFacesVersion() {
		return MY_FACES_VERSION;
	}

}
