/*
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
 package org.ops4j.pax.web.itest.undertow;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.web.itest.base.client.HttpTestClientFactory;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @author Toni Menzel (tonit)
 * @since Mar 3, 2009
 */
@RunWith(PaxExam.class)
public class RootAliasIntegrationTest extends ITestBase {

	private ServiceRegistration<Servlet> servletRoot;
	private ServiceRegistration<Servlet> servletSecond;
	private ServiceReference<HttpService> serviceReference;
	private HttpService httpService;

	@Configuration
	public static Option[] configure() {
		return configureUndertow();
	}

	@Before
	public void setUp() throws Exception {
		waitForServer("http://127.0.0.1:8181/");
		
		initServletListener(null);
		
		servletRoot = registerServletWhiteBoard("/myRoot");
		waitForServletListener();
		servletSecond = registerServletWhiteBoard("/myRoot/second");
		waitForServletListener();
		
		serviceReference = bundleContext.getServiceReference(HttpService.class);
		
		httpService = bundleContext.getService(serviceReference);
		
		registerServlet("/secondRoot");
		waitForServletListener();
		registerServlet("/secondRoot/third");
		waitForServletListener();
	}
	
	private ServiceRegistration<Servlet> registerServletWhiteBoard(final String path) throws ServletException {
		
		Dictionary<String, String> initParams = new Hashtable<String, String>();
		initParams.put("alias", path);

        return bundleContext.registerService(Servlet.class,
                new HttpServlet()
                {

                    /**
                     * 
                     */
                    private static final long serialVersionUID = -4034428893184634308L;

                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
                        resp.getOutputStream().write(path.getBytes());
                    }
                },
                initParams);
        
        }
	
	private void registerServlet(final String path) throws ServletException, NamespaceException {
        httpService.registerServlet(path, new HttpServlet() {

            private static final long serialVersionUID = 7002851015500239901L;

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
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot'",
						resp -> resp.contains("myRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot/second'",
						resp -> resp.contains("myRoot/second"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot/second");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'myRoot'",
						resp -> resp.contains("myRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/myRoot/wrong");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot'",
						resp -> resp.contains("secondRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot/third'",
						resp -> resp.contains("secondRoot/third"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot/third");
		HttpTestClientFactory.createDefaultTestClient()
				.withResponseAssertion("Response must contain 'secondRoot'",
						resp -> resp.contains("secondRoot"))
				.doGETandExecuteTest("http://127.0.0.1:8181/secondRoot/wrong");

//		testClient.testWebPath("http://127.0.0.1:8181/myRoot", "myRoot");
//		testClient.testWebPath("http://127.0.0.1:8181/myRoot/second", "myRoot/second");
//		testClient.testWebPath("http://127.0.0.1:8181/myRoot/wrong", "myRoot");
//		testClient.testWebPath("http://127.0.0.1:8181/secondRoot", "secondRoot");
//		testClient.testWebPath("http://127.0.0.1:8181/secondRoot/third", "secondRoot/third");
//		testClient.testWebPath("http://127.0.0.1:8181/secondRoot/wrong", "secondRoot");
	}
	
	

}
