package org.ops4j.pax.web.itest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import org.ops4j.io.StreamUtils;



public class WebAssertions {

    private static Integer httpPort;

    public static int getHttpPort() {
        if (httpPort == null) {
            String httpPortNumber = System.getProperty("org.osgi.service.http.port", "8181");
            httpPort = Integer.parseInt(httpPortNumber);
        }
        return httpPort;
    }

    public static void assertResourceContainsString(String resource, String expected) {
        try {
            URL url = new URL(String.format("http://localhost:%s/%s", getHttpPort(), resource));
            InputStream is = url.openStream();
            OutputStream os = new ByteArrayOutputStream();
            StreamUtils.copyStream(is, os, true);
            assertThat(os.toString(), containsString(expected));
        }
        catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void assertResourceNotMapped(String resource) {
        try {
            URL url = new URL(String.format("http://localhost:%s/%s", getHttpPort(), resource));
            InputStream is = url.openStream();
            if (is != null) {                
                throw new AssertionError("resource should not be mapped: " + resource);
            }
        }
        catch (FileNotFoundException exc) {
            // expected
        }
        catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

}
