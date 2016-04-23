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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ops4j.pax.exam.OptionUtils.combine;


/**
 * @author Achim Nierbeck
 */
@RunWith(PaxExam.class)
public class WebSocketWhiteBoardIntegrationTest extends ITestBase {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketWhiteBoardIntegrationTest.class);

	@Configuration
	public static Option[] configure() {
		return combine(
				configureWebSocketUndertow());
	}

	@Before
	public void setUp() throws BundleException, InterruptedException {
		LOG.info("Setting up test");

		initWebListener();
		waitForWebListener();
	}


	@Test
//	@Ignore("Only works with an external websocket test tool like 'Simple Websocket client' a chrome extension")
	public void testWebsocket() throws Exception {

	    SimpleWebSocket simpleWebSocket = new SimpleWebSocket();
	    
	    bundleContext.registerService(Object.class.getName(), simpleWebSocket, null);
	    
	    Thread.sleep(1000);
	    
	    
	    
//	    ContextClassLoaderUtils.doWithClassLoader(getClass().getClassLoader(),
//                new Callable<Void>() {
//
//                    @Override
//                    public Void call() throws Exception {
//                        WebSocketClient webSocketClient = new WebSocketClient();
//                        boolean test = webSocketClient.test();
//                        assertTrue(test);
//                        return null;
//                    }
//
//                });
		
	}
	
	
//	public class WebSocketClient {
//	    public boolean test() throws Exception{
//	        
//	        URI uri = URI.create("ws://127.0.0.1:8181/simple/");
//
//	        ClientContainer container = new ClientContainer();
//	        
//            try
//            {
//                // Attempt Connect
//                Session session = container.connectToServer(SimpleWebSocket.class,uri);
//                // Send a message
//                session.getBasicRemote().sendText("Hello");
//                // Close session
//                session.close();
//            }
//            finally
//            {
//                // Force lifecycle stop when done with container.
//                // This is to free up threads and resources that the
//                // JSR-356 container allocates. But unfortunately
//                // the JSR-356 spec does not handle lifecycles (yet)
//                if (container instanceof LifeCycle)
//                {
//                    ((LifeCycle)container).stop();
//                }
//            }
//	        return true;
//	    }
//	}

}

