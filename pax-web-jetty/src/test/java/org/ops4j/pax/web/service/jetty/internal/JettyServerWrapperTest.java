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
package org.ops4j.pax.web.service.jetty.internal;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ops4j.pax.web.service.WebContainerContext;
import org.ops4j.pax.web.service.spi.model.OsgiContextModel;
import org.ops4j.pax.web.service.spi.model.ServerModel;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

//TODO: Re-Enable tests again
@RunWith(MockitoJUnitRunner.class)
public class JettyServerWrapperTest {
	private static final String KNOWN_CONTEXT_NAME = "TestContext";
	private static final String BUNDLE_SYMBOLIC_NAME = "BundleSymbolicName";
	private static final String DEFAULT_AUTH_METHOD = "DDF";
	private static final String DEFAULT_REALM_NAME = "Karaf";
	private static final int NUMBER_OF_CONCURRENT_EXECUTIONS = 2;
	private static final int REPETITIONS_OF_MULTI_THREADED_TEST = 1000;
	@Mock
	private ServerModel serverModelMock;
	@Mock
	private OsgiContextModel contextModelMock;
	@Mock
	private WebContainerContext httpContextMock;
	@Mock
	private Bundle bundleMock;
	@Mock
	private BundleContext bundleContextMock;
	private volatile Exception exceptionInRunnable;

	@Before
	public void mockIt() {
//		when(contextModelMock.getContextName()).thenReturn(KNOWN_CONTEXT_NAME);
//		when(contextModelMock.getHttpContext()).thenReturn(httpContextMock);
//		when(contextModelMock.getBundle()).thenReturn(bundleMock);
//		doCallRealMethod().when(contextModelMock).getRealmName();
//		doCallRealMethod().when(contextModelMock).getAuthMethod();
//		doCallRealMethod().when(contextModelMock).setRealmName(anyString());
//		doCallRealMethod().when(contextModelMock).setAuthMethod(anyString());
//		when(bundleMock.getHeaders()).thenReturn(
//				new Hashtable<>());
//		when(bundleMock.getSymbolicName()).thenReturn(BUNDLE_SYMBOLIC_NAME);
//		when(bundleMock.getBundleContext()).thenReturn(bundleContextMock);
//		when(bundleContextMock.getBundle()).thenReturn(bundleMock);
	}

    @Ignore
	@SuppressWarnings("unchecked")
	@Test
	public void getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelSingleThreaded()
			throws Exception {
//		final PaxWebJettyServer jettyServerWrapperUnderTest = new PaxWebJettyServer(
//				serverModelMock, new QueuedThreadPool());
//		try {
//			jettyServerWrapperUnderTest.start();
//			HttpServiceContext context = jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
//			context.start();
//			context = jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
//			context.start();
//
//			verify(bundleContextMock, times(1)).registerService(
//					same(ServletContext.class), any(ServletContext.class),
//					any(Dictionary.class));
//		} finally {
//			jettyServerWrapperUnderTest.stop();
//		}
	}

    @Ignore
	@SuppressWarnings("unchecked")
	@Test
	public void getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelMultiThreaded()
			throws Exception {
//		final PaxWebJettyServer jettyServerWrapperUnderTest = new PaxWebJettyServer(
//				serverModelMock, new QueuedThreadPool());
//		try {
//			jettyServerWrapperUnderTest.start();
//			final CountDownLatch countDownLatch = new CountDownLatch(1);
//			final Runnable getOrCreateContextRunnable = new Runnable() {
//				@Override
//				public void run() {
//					//CHECKSTYLE:OFF
//					try {
//						countDownLatch.await();
//						HttpServiceContext context = jettyServerWrapperUnderTest
//								.getOrCreateContext(contextModelMock);
//						context.start();
//					} catch (final InterruptedException ex) {
//						// ignore
//					} catch (final Exception ex) {
//						exceptionInRunnable = ex;
//					}
//					//CHECKSTYLE:ON
//				}
//			};
//			final ExecutorService executor = Executors
//					.newFixedThreadPool(NUMBER_OF_CONCURRENT_EXECUTIONS);
//			for (int i = 0; i < NUMBER_OF_CONCURRENT_EXECUTIONS; i++) {
//				executor.execute(getOrCreateContextRunnable);
//			}
//			countDownLatch.countDown();
//			executor.shutdown();
//			final boolean terminated = executor.awaitTermination(10,
//					TimeUnit.SECONDS);
//			if (exceptionInRunnable != null) {
//				try {
//					throw exceptionInRunnable;
//				} finally {
//					exceptionInRunnable = null;
//				}
//			}
//			assertTrue("could not shutdown the executor within the timeout",
//					terminated);
//
//			verify(bundleContextMock, times(1)).registerService(
//					same(ServletContext.class), any(ServletContext.class),
//					any(Dictionary.class));
//		} finally {
//			jettyServerWrapperUnderTest.stop();
//		}
	}

	// CHECKSTYLE:OFF
	@Test
	public void executeMultiThreadedTestMultipleTimes() throws Throwable {
		int i = 0;
		try {
			for (; i < REPETITIONS_OF_MULTI_THREADED_TEST; i++) {
				getOrCreateContextDoesNotRegisterMultipleServletContextsForSameContextModelMultiThreaded();
				reset(bundleContextMock);
				//TODO: removed unneeded stubbing
				//when(bundleContextMock.getBundle()).thenReturn(bundleMock);
			}
		} catch (final Throwable ex) {
			System.out.println("Broken in Run #" + i);
			throw ex;
		}
	}
	//CHECKSTYLE:ON

    @Ignore
	@Test
	public void sequenceOfGetOrCreateContextGetContextRemoveContext()
			throws Exception {
//		final PaxWebJettyServer jettyServerWrapperUnderTest = new PaxWebJettyServer(
//				serverModelMock, new QueuedThreadPool());
//		try {
//			jettyServerWrapperUnderTest.start();
//			jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
//			final HttpServiceContext httpServiceContext = jettyServerWrapperUnderTest
//					.getContext(httpContextMock);
//			jettyServerWrapperUnderTest.removeContext(httpContextMock, false);
//			final HttpServiceContext httpServiceContextAfterRemoved = jettyServerWrapperUnderTest
//					.getContext(httpContextMock);
//
//			assertNotNull(httpServiceContext);
//			assertNull(httpServiceContextAfterRemoved);
//		} finally {
//			jettyServerWrapperUnderTest.stop();
//		}
	}

//	@Test
//	public void registrationAndUnregistrationOfTwoServletsThereShouldBeNoContexts()
//			throws Exception {
//		JettyServerImpl server = new JettyServerImpl(serverModelMock, null);
//		server.start();
//		try {
//			Bundle testBundle = mock(Bundle.class);
//			OsgiContextModel contextModel = new OsgiContextModel(httpContextMock,
//					testBundle, getClass().getClassLoader(), null);
//			ServletModel servletModel1 = new ServletModel(contextModel, new DefaultServlet(),
//					"/s1", null, null, null);
//			ServletModel servletModel2 = new ServletModel(contextModel, new DefaultServlet(),
//					"/s2", null, null, null);
//			assertNull(server.getServer().getContext(httpContextMock));
//			server.addServlet(servletModel1);
//			assertNotNull(server.getServer().getContext(httpContextMock));
//			server.addServlet(servletModel2);
//			assertNotNull(server.getServer().getContext(httpContextMock));
//			server.removeServlet(servletModel1);
//			assertNotNull(server.getServer().getContext(httpContextMock));
//			server.removeServlet(servletModel2);
//			assertNull(server.getServer().getContext(httpContextMock));
//		} finally {
//			server.stop();
//		}
//	}

	@Ignore
	@Test
	public void testDefaultAuthMethod()
			throws Exception {
//		final PaxWebJettyServer jettyServerWrapperUnderTest = new PaxWebJettyServer(
//				serverModelMock, new QueuedThreadPool());
//		try {
////			assertNull(contextModelMock.getAuthMethod());
//			jettyServerWrapperUnderTest.setDefaultAuthMethod(DEFAULT_AUTH_METHOD);
//			jettyServerWrapperUnderTest.start();
//			HttpServiceContext context = jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
//			context.start();
////			assertTrue(DEFAULT_AUTH_METHOD.equals(contextModelMock.getAuthMethod()));
//		} finally {
//			jettyServerWrapperUnderTest.stop();
//		}
	}

    @Ignore
	@Test
	public void testDefaultRealmName()
			throws Exception {
//		final PaxWebJettyServer jettyServerWrapperUnderTest = new PaxWebJettyServer(
//				serverModelMock, new QueuedThreadPool());
//		try {
////			assertNull(contextModelMock.getRealmName());
//			jettyServerWrapperUnderTest.setDefaultRealmName(DEFAULT_REALM_NAME);
//			jettyServerWrapperUnderTest.start();
//			HttpServiceContext context = jettyServerWrapperUnderTest.getOrCreateContext(contextModelMock);
//			context.start();
////			assertTrue(DEFAULT_REALM_NAME.equals(contextModelMock.getRealmName()));
//		} finally {
//			jettyServerWrapperUnderTest.stop();
//		}
	}
}
