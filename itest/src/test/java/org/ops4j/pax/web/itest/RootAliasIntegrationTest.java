package org.ops4j.pax.web.itest;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class RootAliasIntegrationTest extends ITestBase {

	private ServiceRegistration servletRoot;
	private ServiceRegistration servletSecond;
	private ServiceReference serviceReference;
	private HttpService httpService;

	@Configuration
	public static Option[] configure() {
		return configureJetty();
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8181/");
		
		initServletListener();
		
		servletRoot = registerServletWhiteBoard("/myRoot");
		waitForServletListener();
		servletSecond = registerServletWhiteBoard("/myRoot/second");
		waitForServletListener();
		
		serviceReference = bundleContext.getServiceReference("org.osgi.service.http.HttpService");
		
		httpService = (HttpService) bundleContext.getService(serviceReference);
		
		registerServlet("/secondRoot");
		waitForServletListener();
		registerServlet("/secondRoot/third");
		waitForServletListener();
	}
	
	private ServiceRegistration registerServletWhiteBoard(final String path) throws ServletException {
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", path);

        return bundleContext.registerService(Servlet.class.getName(),
        		new HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getOutputStream().write(path.getBytes());
            }
        }, initParams);
        
    }
	
	private void registerServlet(final String path) throws ServletException, NamespaceException {
        httpService.registerServlet(path, new HttpServlet() {

            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                resp.getOutputStream().write(path.getBytes());
            }
        }, null, null);
        System.out.println("registered: " + path);
    }

    private void unregisterServlet(String path) {
        httpService.unregister(path);
        System.out.println("unregistered: " + path);
    }

	@After
	public void tearDown() throws BundleException {
		servletRoot.unregister();
		servletSecond.unregister();
		

		unregisterServlet("/secondRoot");
		unregisterServlet("/secondRoot/third");
	}

	@Test
	public void testWhiteBoardSlash() throws Exception {
		testWebPath("http://127.0.0.1:8181/myRoot", "myRoot");

		testWebPath("http://127.0.0.1:8181/myRoot/second", "myRoot/second");
		
		testWebPath("http://127.0.0.1:8181/myRoot/wrong", "myRoot");
		
		testWebPath("http://127.0.0.1:8181/secondRoot", "secondRoot");

		testWebPath("http://127.0.0.1:8181/secondRoot/third", "secondRoot/third");
		
		testWebPath("http://127.0.0.1:8181/secondRoot/wrong", "secondRoot");

	}
	
	

}
